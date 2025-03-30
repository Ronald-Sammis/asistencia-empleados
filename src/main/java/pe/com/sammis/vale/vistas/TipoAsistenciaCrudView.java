package pe.com.sammis.vale.vistas;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;
import pe.com.sammis.vale.models.TipoAsistencia;
import pe.com.sammis.vale.repositories.TipoAsistenciaRepository;

@CssImport("./themes/mi-tema/styles.css")
@Route(value = "tipo", layout = MainLayout.class)
public class TipoAsistenciaCrudView extends VerticalLayout {

    private Grid<TipoAsistencia> grid = new Grid<>(TipoAsistencia.class);
    private Button addButton = new Button( "Nuevo");
    private TipoAsistenciaRepository repository;

    private Dialog formDialog = new Dialog();
    private Dialog confirmDialog = new Dialog();
    private TextField nombreField = new TextField("Nombre");
    private TextField colorHexField = new TextField("Código HEX");
    private Input colorPicker = new Input();
    private Button saveButton = new Button("Guardar");
    private Button cancelButton = new Button("Cancelar");
    private TipoAsistencia currentTipoAsistencia;
    private boolean notificationShown = false;

    private TextField searchField = new TextField();

    @Autowired
    public TipoAsistenciaCrudView(TipoAsistenciaRepository repository) {
        addClassName("main-view");

        this.repository = repository;
        add(new H2("Tipos de asistencia"));

        configureColorPicker();
        configureGrid();
        createForm();
        configureToolbar();
        updateList();
    }

    private void configureColorPicker() {
        colorPicker.setType("color");
        colorPicker.getStyle().set("width", "100%");

        colorPicker.addValueChangeListener(event -> {
            if (!event.getValue().equals(colorHexField.getValue())) {
                colorHexField.setValue(event.getValue());
            }
        });

        colorHexField.addValueChangeListener(event -> {
            if (event.getValue().matches("^#([A-Fa-f0-9]{6})$")) {
                colorPicker.setValue(event.getValue());
            }
        });
    }

    private void configureToolbar() {
        searchField.setPlaceholder("Buscar...");
        searchField.setClearButtonVisible(true);
        searchField.setWidth("250px");

        addButton.addClickListener(e -> openForm(new TipoAsistencia()));
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.setWidth("75px");

        HorizontalLayout toolbar = new HorizontalLayout(addButton, searchField);
        add(toolbar, grid);
    }

    private void configureGrid() {

        grid.setColumns( "nombre");
        grid.getColumnByKey("nombre").setAutoWidth(true);


        grid.setWidth("350px");
        grid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_COLUMN_BORDERS);

        grid.addComponentColumn(tipo -> {
            Div colorPreview = new Div();

            // Asegurarse de que el color en la vista previa tenga el '#' si es necesario
            String colorHex = tipo.getColorHex();
            if (colorHex != null && !colorHex.startsWith("#")) {
                colorHex = "#" + colorHex;  // Agregar '#' si no está presente
            }

            // Usar el valor con '#' para la vista previa
            colorPreview.getStyle().set("background-color", colorHex);
            colorPreview.getStyle().set("width", "20px");
            colorPreview.getStyle().set("height", "20px");
            colorPreview.getStyle().set("border-radius", "50%");
            return colorPreview;
        }).setHeader("Color")
                .setAutoWidth(true);

        grid.addComponentColumn(tipo -> {
            Button editButton = new Button(VaadinIcon.EDIT.create());
            editButton.addClickListener(event -> openForm(tipo));
            editButton.getStyle()
                    .set("background-color", "var(--lumo-warning-color)") // Fondo amarillo
                    .set("color", "black"); // Texto negro
            editButton.setWidth("75px");
            return editButton;
        }).setHeader("Editar")
                .setAutoWidth(true);

        grid.addComponentColumn(tipo -> {
            Button deleteButton = new Button(VaadinIcon.TRASH.create());
            deleteButton.addClickListener(event -> confirmDelete(tipo));
            deleteButton.getStyle()
                    .set("background-color", "var(--lumo-error-color)") // Fondo amarillo
                    .set("color", "white"); // Texto negro
            deleteButton.setWidth("75px");
            return deleteButton;
        }).setHeader("Eliminar")
                .setAutoWidth(true);
    }


    private void createForm() {
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(e -> saveTipoAsistencia());
        cancelButton.addClickListener(e -> formDialog.close());

        VerticalLayout formLayout = new VerticalLayout();
        formLayout.setSpacing(true);
        formLayout.setPadding(true);
        formLayout.setAlignItems(FlexComponent.Alignment.STRETCH);

        formLayout.add(nombreField);
        formLayout.add(colorHexField); // Código HEX primero
        formLayout.add(colorPicker);   // Color Picker al final
        formLayout.add(new HorizontalLayout(saveButton, cancelButton));

        formDialog.add(formLayout);
    }

    private void openForm(TipoAsistencia tipoAsistencia) {
        currentTipoAsistencia = tipoAsistencia;
        nombreField.setValue(tipoAsistencia.getNombre() != null ? tipoAsistencia.getNombre() : "");

        // Mostrar el color exactamente como está, con el '#'
        String color = tipoAsistencia.getColorHex() != null ? tipoAsistencia.getColorHex() : "#000000";
        colorHexField.setValue(color); // Mostrar el valor tal cual, incluyendo '#'
        colorPicker.setValue(color); // Configurar el picker con el color
        formDialog.open();
        notificationShown = false;
    }



    private void saveTipoAsistencia() {
        if (nombreField.isEmpty() || colorHexField.isEmpty()) {
            if (!notificationShown) {
                Notification notification = Notification.show("Por favor, complete los campos obligatorios.", 3000, Notification.Position.BOTTOM_CENTER);
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                notification.addOpenedChangeListener(event -> notificationShown = false);
                notificationShown = true;
            }
        } else {
            currentTipoAsistencia.setNombre(nombreField.getValue());

            // Guardar el valor del color exactamente tal como se ingresa (con '#' si está presente)
            String color = colorHexField.getValue();  // No hacer ningún cambio ni manipulación
            currentTipoAsistencia.setColorHex(color); // Guardar el valor tal cual

            repository.save(currentTipoAsistencia);
            updateList();
            formDialog.close();
            notificationShown = false;

            Notification notification = Notification.show("Tipo de asistencia guardado correctamente: " + currentTipoAsistencia.getNombre().toUpperCase(), 3000, Notification.Position.BOTTOM_CENTER);
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            notification.addOpenedChangeListener(event -> notificationShown = false);
        }
    }




    private void confirmDelete(TipoAsistencia tipoAsistencia) {
        confirmDialog.removeAll();
        confirmDialog.add("¿Está seguro de que desea eliminar este tipo de asistencia?");

        Button confirmButton = new Button("Sí", event -> {
            repository.delete(tipoAsistencia);
            updateList();
            confirmDialog.close();
            Notification.show("Tipo de asistencia eliminado: " + tipoAsistencia.getNombre().toUpperCase(), 3000, Notification.Position.BOTTOM_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });
        confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("No", event -> confirmDialog.close());
        confirmDialog.add(new HorizontalLayout(confirmButton, cancelButton));
        confirmDialog.open();
    }

    private void updateList() {
        grid.setItems(repository.findAll());
    }

    private void filterList(String searchTerm) {
        ListDataProvider<TipoAsistencia> dataProvider = (ListDataProvider<TipoAsistencia>) grid.getDataProvider();
        dataProvider.setFilter(tipoAsistencia ->
                tipoAsistencia.getNombre().toLowerCase().contains(searchTerm.toLowerCase()) ||
                        tipoAsistencia.getColorHex().toLowerCase().contains(searchTerm.toLowerCase()));
        grid.getDataProvider().refreshAll();
    }
}
