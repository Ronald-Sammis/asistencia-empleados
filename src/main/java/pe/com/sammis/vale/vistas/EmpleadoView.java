package pe.com.sammis.vale.vistas;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import org.json.JSONException;
import org.json.JSONObject;
import pe.com.sammis.vale.models.Empleado;
import pe.com.sammis.vale.services.interfaces.IEmpleadoService;

import java.util.List;

@Route(value = "empleado2", layout = MainLayout.class)
public class EmpleadoView extends VerticalLayout {

    private final Grid<Empleado> grid = new Grid<>(Empleado.class);
    private final FormLayout form = new FormLayout();
    private final TextField dniField = new TextField("DNI");
    private final TextField nombreField = new TextField("Nombre");
    private final TextField apellidoField = new TextField("Apellido");
    private final Button saveButton = new Button("Guardar");
    private final Button cancelButton = new Button("Cancelar");
    private final Button newButton = new Button("Nuevo");
    private final TextField searchField = new TextField();
    private final Dialog formDialog = new Dialog();
    private final Dialog loadingDialog = new Dialog();
    private Empleado currentEmpleado;


    private IEmpleadoService empleadoService;

    public EmpleadoView(IEmpleadoService empleadoService) {
        this.empleadoService = empleadoService;
        setUpToolbar();
        setUpGrid();
        setUpForm();
        setUpDialog();
        updateGrid();
        setupLoadingDialog();
    }

    private void setUpGrid() {
        grid.setColumns("id", "dni", "nombre", "apellido");

        // Columna de acciones (Editar + Eliminar)
        grid.addComponentColumn(empleado -> {
            Button editButton = new Button("Editar");
            editButton.addClickListener(e -> openFormForEditEmpleado(empleado));

            Button deleteButton = new Button("Eliminar");
            deleteButton.addClickListener(e -> deleteEmpleado(empleado));

            return new HorizontalLayout(editButton, deleteButton);
        }).setHeader("Acciones");

        add(grid);
    }

    private void setUpForm() {



        saveButton.addClickListener(e -> saveEmpleado());
        cancelButton.addClickListener(e -> cancelForm());

        // Agregar campo de búsqueda dentro del formulario de nuevo empleado
        TextField searchEmpleadoField = new TextField("Buscar Empleado");
        searchEmpleadoField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchEmpleadoField.setClearButtonVisible(true);
        searchEmpleadoField.addValueChangeListener(e -> searchEmpleado(e.getValue()));  // Método para buscar empleados

        // Crear el layout vertical para organizar los elementos en una columna
        VerticalLayout formLayout = new VerticalLayout();
        formLayout.add(searchEmpleadoField, dniField, nombreField, apellidoField);
        formLayout.setAlignItems(Alignment.BASELINE);  // Alineación de todos los elementos al inicio

        // Crear un layout horizontal solo para los botones
        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, cancelButton);
        buttonLayout.setSpacing(true);  // Espaciado entre los botones

        formLayout.add(buttonLayout);  // Agregar los botones al formulario
        form.add(formLayout);  // Agregar el layout al formulario
        form.setVisible(false);
    }


    private void setUpToolbar() {
        searchField.setPlaceholder("Buscar...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);

        newButton.addClickListener(e -> openFormForNewEmpleado());

        HorizontalLayout toolbar = new HorizontalLayout(newButton, searchField);
        add(toolbar);
    }

    private void setUpDialog() {
        formDialog.add(form);
        formDialog.setCloseOnOutsideClick(false);
    }

    private void openFormForNewEmpleado() {
        currentEmpleado = null;  // Asegura que se cree un nuevo empleado
        dniField.clear();
        nombreField.clear();
        apellidoField.clear();
        form.setVisible(true);
        formDialog.open();
    }


    private void openFormForEditEmpleado(Empleado empleado) {
        currentEmpleado = empleado;  // Asocia el empleado a editar
        dniField.setValue(empleado.getDni());
        nombreField.setValue(empleado.getNombre());
        apellidoField.setValue(empleado.getApellido());
        form.setVisible(true);
        formDialog.open();
    }


    private void deleteEmpleado(Empleado empleado) {
        confirmDelete(empleado);
    }

    private void confirmDelete(Empleado empleado) {
        Dialog confirmDialog = new Dialog();
        confirmDialog.setHeaderTitle("Confirmar eliminación");

        confirmDialog.add("¿Estás seguro de que deseas eliminar al empleado: " + empleado.getNombre() + "?");

        Button confirmButton = new Button("Eliminar", event -> {
            empleadoService.deleteById(empleado.getId());
            updateGrid();
            grid.getDataProvider().refreshAll();
            confirmDialog.close();
        });

        Button cancelButton = new Button("Cancelar", event -> confirmDialog.close());

        confirmDialog.getFooter().add(cancelButton, confirmButton);
        confirmDialog.open();
    }

    private void updateGrid() {
        List<Empleado> empleados = empleadoService.findAll();
        grid.setItems(empleados);
    }

    private void saveEmpleado() {
        loadingDialog.open(); // Mostrar el diálogo de carga

        try {
            String dni = dniField.getValue();
            String nombre = nombreField.getValue();
            String apellido = apellidoField.getValue();

            if (currentEmpleado != null) {
                currentEmpleado.setDni(dni);
                currentEmpleado.setNombre(nombre);
                currentEmpleado.setApellido(apellido);
                empleadoService.save(currentEmpleado);
            } else {
                Empleado nuevoEmpleado = new Empleado();
                nuevoEmpleado.setDni(dni);
                nuevoEmpleado.setNombre(nombre);
                nuevoEmpleado.setApellido(apellido);
                empleadoService.save(nuevoEmpleado);
            }

            updateGrid();
            formDialog.close();
        } catch (Exception e) {
            Notification.show("Error al guardar el empleado", 3000, Notification.Position.MIDDLE);
            e.printStackTrace();
        } finally {
            loadingDialog.close(); // Cerrar el diálogo de carga siempre
        }
    }



    private void cancelForm() {
        dniField.clear();
        nombreField.clear();
        apellidoField.clear();
        formDialog.close();
    }

    private void searchEmpleado(String dni) {

            if (dni.length() == 8) {
                try {
                    String responseData = empleadoService.consultaSunat(dni); // Llamar al Service
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




    private void setupLoadingDialog() {
        loadingDialog.setCloseOnEsc(false);
        loadingDialog.setCloseOnOutsideClick(false);

        ProgressBar progressBar = new ProgressBar();
        progressBar.setIndeterminate(true);

        Span message = new Span("Procesando... Por favor espere.");
        message.getStyle().set("font-weight", "bold");

        VerticalLayout layout = new VerticalLayout(progressBar, message);
        layout.setAlignItems(Alignment.CENTER);
        layout.setPadding(true);

        loadingDialog.add(layout);
        add(loadingDialog); // ¡IMPORTANTE!
    }

}
