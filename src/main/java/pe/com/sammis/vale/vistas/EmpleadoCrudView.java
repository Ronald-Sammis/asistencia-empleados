package pe.com.sammis.vale.vistas;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
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
import com.vaadin.flow.theme.lumo.Lumo;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import pe.com.sammis.vale.models.Empleado;
import pe.com.sammis.vale.repositories.EmpleadoRepository;
import pe.com.sammis.vale.services.APISunatServiceImpl;
import org.json.JSONObject;

import java.util.Optional;


@CssImport("./themes/mi-tema/styles.css")
@Route(value = "empleado", layout = MainLayout.class)
public class EmpleadoCrudView extends VerticalLayout {

    private Grid<Empleado> grid = new Grid<>(Empleado.class);
    private Button addButton = new Button("Nuevo");
    private EmpleadoRepository repository;
    private Dialog formDialog = new Dialog();
    private Dialog confirmDialog = new Dialog();
    private TextField nombreField = new TextField("Nombre");
    private TextField apellidoField = new TextField("Apellido");
    private TextField dniField = new TextField("DNI");
    private Button saveButton = new Button("Guardar");
    private Button cancelButton = new Button("Cancelar");
    private Empleado currentEmpleado;
    private boolean notificationShown = false;
    private TextField searchField = new TextField();
    private TextField searchFieldDni = new TextField();
    private APISunatServiceImpl apiSunatService;

    @Autowired
    public EmpleadoCrudView(EmpleadoRepository repository, APISunatServiceImpl apiSunatService) {

        addClassName("main-view");
        this.repository = repository;
        this.apiSunatService = apiSunatService;
        add(new H2("Registro de empleados"));
        configureGrid();
        createForm();
        configureToolbar();
        updateList();
    }

    private void configureToolbar() {
        configureSearchField();
        configureAddButton();
        HorizontalLayout toolbar = new HorizontalLayout(addButton, searchField);
        add(toolbar, grid);
    }

    private void configureSearchField() {
        searchField.setPlaceholder("Buscar...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);
        searchField.setWidth("250px");
        searchField.addValueChangeListener(event -> filterList(event.getValue()));
    }

    private void configureAddButton() {
        addButton.setText("Nuevo");
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.setWidth("75px");
        addButton.addClickListener(e -> openForm(new Empleado()));
    }


    private void configureGrid() {
        configureGridProperties();
        addDataColumns();
        addActionColumns();
    }

    private void configureGridProperties() {
        grid.setWidth("525px");
        grid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_COLUMN_BORDERS);
        grid.setColumns(); // Evita que Vaadin agregue columnas automáticamente
    }

    private void addDataColumns() {
        grid.addColumn("dni").setAutoWidth(true);
        grid.addColumn(empleado -> empleado.getNombre() + " " + empleado.getApellido())
                .setHeader("Nombres")
                .setAutoWidth(true);
    }

    private void addActionColumns() {
        grid.addComponentColumn(this::createEditButton).setHeader("Editar").setAutoWidth(true);
        grid.addComponentColumn(this::createDeleteButton).setHeader("Eliminar").setAutoWidth(true);
    }

    private Button createEditButton(Empleado empleado) {
        Button editButton = new Button(VaadinIcon.EDIT.create());
        editButton.addClickListener(event -> openForm(empleado));
        styleButton(editButton, "var(--lumo-warning-color)", "black");
        return editButton;
    }

    private Button createDeleteButton(Empleado empleado) {
        Button deleteButton = new Button(VaadinIcon.TRASH.create());
        deleteButton.addClickListener(event -> confirmDelete(empleado));
        styleButton(deleteButton, "var(--lumo-error-color)", "white");
        return deleteButton;
    }

    private void styleButton(Button button, String bgColor, String textColor) {
        button.setWidth("75px");
        button.getStyle()
                .set("background-color", bgColor)
                .set("color", textColor);
    }


    private void createForm() {
        // Campo de búsqueda con icono de lupa
        TextField searchFieldDni = new TextField();
        searchFieldDni.setPlaceholder("Ingrese DNI...");
        searchFieldDni.setClearButtonVisible(true);
        searchFieldDni.setPrefixComponent(new Icon(VaadinIcon.SEARCH)); // Icono de lupa
        searchFieldDni.addValueChangeListener(e -> buscarDatosPorDNI(e.getValue())); // Llamada automática

        // Botones
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(e -> saveEmpleado());
        cancelButton.addClickListener(e -> formDialog.close());

        // Layout del formulario
        VerticalLayout formLayout = new VerticalLayout(
                searchFieldDni,  // Campo de búsqueda con lupa
                dniField,
                nombreField,
                apellidoField,
                new HorizontalLayout(saveButton, cancelButton)
        );

        formDialog.add(formLayout);
    }


    private void openForm(Empleado empleado) {
        searchFieldDni.clear();
        dniField.clear();
        nombreField.clear();
        apellidoField.clear();

        currentEmpleado = empleado;
        nombreField.setValue(empleado.getNombre() != null ? empleado.getNombre() : "");
        apellidoField.setValue(empleado.getApellido() != null ? empleado.getApellido() : "");
        formDialog.open();
        notificationShown = false; // Reset para permitir nuevas notificaciones
    }

    private void saveEmpleado() {
        if (nombreField.isEmpty() || apellidoField.isEmpty() || dniField.isEmpty()) {
            showNotification("Por favor, complete los campos obligatorios.", NotificationVariant.LUMO_ERROR);
            return;
        }

        String dni = dniField.getValue();

        // Verificamos si ya existe un empleado con el mismo DNI
        Optional<Empleado> existingEmpleado = repository.findByDni(dni);
        if (existingEmpleado.isPresent()) {
            // Si ya existe un empleado con ese DNI, mostramos un mensaje de error
            showNotification("El DNI ya está registrado. Por favor, ingrese un DNI único.", NotificationVariant.LUMO_ERROR);
            return;
        }

        // Si el DNI no existe, asignamos los valores al empleado actual
        currentEmpleado.setDni(dni);
        currentEmpleado.setNombre(nombreField.getValue());
        currentEmpleado.setApellido(apellidoField.getValue());

        // Guardamos el empleado
        repository.save(currentEmpleado);

        // Actualizamos la lista y cerramos el formulario
        updateList();
        formDialog.close();
        notificationShown = false; // Reset para permitir nuevas notificaciones

        // Mostramos la notificación de éxito
        showNotification("Empleado guardado correctamente: " +
                        currentEmpleado.getNombre().toUpperCase() + " " +
                        currentEmpleado.getApellido().toUpperCase(),
                NotificationVariant.LUMO_SUCCESS);
    }


    private void showNotification(String message, NotificationVariant variant) {
        if (!notificationShown) {
            Notification notification = Notification.show(message, 3000, Notification.Position.BOTTOM_CENTER);
            notification.addThemeVariants(variant);
            notification.addOpenedChangeListener(event -> notificationShown = false);
            notificationShown = true;
        }
    }


    private void
    confirmDelete(Empleado empleado) {
        confirmDialog.removeAll();
        confirmDialog.add("¿Está seguro de que desea eliminar este empleado?");

        Button confirmButton = new Button("Sí", event -> {
            repository.delete(empleado);
            updateList();
            confirmDialog.close();
            notificationShown = false; // Reset para permitir nuevas notificaciones
            Notification notification = Notification.show("Empleado eliminado:" + empleado.getNombre() + " " + empleado.getApellido(), 3000, Notification.Position.BOTTOM_CENTER);
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            notification.addOpenedChangeListener(e -> notificationShown = false);
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
        ListDataProvider<Empleado> dataProvider = (ListDataProvider<Empleado>) grid.getDataProvider();
        if (searchTerm.isEmpty()) {
            updateList();
        } else {
            dataProvider.setFilter(empleado ->
                    empleado.getNombre().toLowerCase().contains(searchTerm.toLowerCase()) ||
                            empleado.getApellido().toLowerCase().contains(searchTerm.toLowerCase())
            );
            grid.getDataProvider().refreshAll();
        }
    }


    private void buscarDatosPorDNI(String dni) {


        if (dni.length() == 8) {
            try {
                String responseData = apiSunatService.consultarDocumento(dni); // Llamar al Service
                if (responseData != null) {
                    try {
                        JSONObject json = new JSONObject(responseData);
                        dniField.setValue(dni);
                        nombreField.setValue(json.optString("nombres", ""));
                        apellidoField.setValue(json.optString("apellidoPaterno", "") + " " + json.optString("apellidoMaterno", ""));
                    } catch (JSONException e) {
                        Notification.show("Error al procesar la respuesta del servidor. Respuesta no es válida.", 3000, Notification.Position.MIDDLE);
                        e.printStackTrace();
                    }
                } else {
                    Notification.show("No se encontraron datos", 3000, Notification.Position.MIDDLE);
                }
            } catch (Exception e) {
                Notification.show("Hubo un error al realizar la consulta", 3000, Notification.Position.MIDDLE);
                e.printStackTrace();
            }
        } else {
            Notification.show("El DNI debe tener 8 caracteres", 3000, Notification.Position.MIDDLE);
        }
    }


}
