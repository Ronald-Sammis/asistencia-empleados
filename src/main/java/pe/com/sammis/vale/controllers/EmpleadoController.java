package pe.com.sammis.vale.controllers;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import org.json.JSONException;
import org.json.JSONObject;
import pe.com.sammis.vale.models.Empleado;
import pe.com.sammis.vale.services.Interfaces.IEmpleadoService;
import pe.com.sammis.vale.services.implement.APISunatServiceImpl;
import pe.com.sammis.vale.vistas.EmpleadoView;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class EmpleadoController {

    private final IEmpleadoService empleadoService;
    private final APISunatServiceImpl apiSunatService;
    private final EmpleadoView view;
    private Dialog confirmDialog = new Dialog();
    private boolean notificationShown = false;
    private Empleado empleado=new Empleado();


    public EmpleadoController(IEmpleadoService empleadoService, APISunatServiceImpl apiSunatService, EmpleadoView view) {
        this.empleadoService = empleadoService;
        this.apiSunatService = apiSunatService;
        this.view = view;
    }

    public void editarForm(Empleado empleado) {
        // Si es null, es un nuevo empleado
        this.empleado = (empleado != null) ? empleado : new Empleado();  // Asignamos el empleado actual o uno nuevo
        view.setFormFields(this.empleado.getNombre(), this.empleado.getApellido(), this.empleado.getDni()); // Cargar los valores
        view.openFormDialog(); // Abre el formulario
    }


    public void saveEmpleadoNuevo() {
        // Si estamos editando un empleado, verificamos si el DNI ha cambiado
        if (empleado.getId() != null) {
            // Estamos editando un empleado
            System.out.println("Editando empleado con ID: " + empleado.getId() + ", DNI: " + empleado.getDni());
            Optional<Empleado> empleadoExistente = empleadoService.findByDni(empleado.getDni());

            // Si el empleado con el mismo DNI existe, pero no es el mismo empleado, mostramos un error
            if (empleadoExistente.isPresent() && !empleadoExistente.get().getId().equals(empleado.getId())) {
                view.showNotification("El DNI ya está registrado.", NotificationVariant.LUMO_ERROR);
                return;
            }
        } else {
            // Si estamos creando un nuevo empleado
            System.out.println("Creando nuevo empleado, DNI: " + empleado.getDni());
            Optional<Empleado> empleadoExistente = empleadoService.findByDni(empleado.getDni());

            // Si el DNI ya está registrado, mostramos un error
            if (empleadoExistente.isPresent()) {
                view.showNotification("El DNI ya está registrado.", NotificationVariant.LUMO_ERROR);
                return;
            }
        }

        // Asignar los valores del formulario al empleado
        empleado.setNombre(view.getNombre());
        empleado.setApellido(view.getApellido());
        empleado.setDni(view.getDni());

        // Verificar que los campos no estén vacíos
        if (empleado.getNombre().isEmpty() || empleado.getApellido().isEmpty() || empleado.getDni().isEmpty()) {
            view.showNotification("Por favor, complete los campos obligatorios.", NotificationVariant.LUMO_ERROR);
            return;
        }

        // Guardar el empleado (crear o editar)
        empleadoService.save(empleado);

        // Mostrar mensaje de éxito
        view.showNotification("Empleado guardado correctamente.", NotificationVariant.LUMO_SUCCESS);

        // Actualizar la lista y cerrar el formulario
        view.updateList();
        view.closeFormDialog();
    }


    public void editarEmpleado(Empleado empleado) {
        // Prepara el formulario de edición con los datos del empleado
        view.setFormFields(empleado.getNombre(), empleado.getApellido(), empleado.getDni());
        view.openFormDialog(); // Abre el diálogo de edición
    }





    public void eliminarEmpleado(Empleado empleado) {

        confirmDialog.removeAll();
        confirmDialog.add("¿Está seguro de que desea eliminar este empleado?");

        Button confirmButton = new Button("Sí", event -> {
            empleadoService.deleteById(empleado.getId());
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


    public List<Empleado> findAllEmpleados() {
        return empleadoService.findAll();
    }

    public void updateList() {
        List<Empleado> empleados = empleadoService.findAll();
        view.updateGrid(empleados);
    }

    public void filterList(String searchTerm) {
        List<Empleado> allEmpleados = empleadoService.findAll(); // Obtienes todos los empleados
        if (searchTerm == null || searchTerm.isEmpty()) {
            view.updateGrid(allEmpleados); // Si no hay búsqueda, mostramos todos los empleados
        } else {
            // Filtrar en memoria usando el término de búsqueda
            List<Empleado> filteredList = allEmpleados.stream()
                    .filter(empleado -> empleado.getNombre().toLowerCase().contains(searchTerm.toLowerCase()) ||
                            empleado.getApellido().toLowerCase().contains(searchTerm.toLowerCase()) ||
                            empleado.getDni().contains(searchTerm)) // Puedes agregar más filtros si es necesario
                    .collect(Collectors.toList());

            view.updateGrid(filteredList); // Actualizas el Grid con los empleados filtrados
        }
    }




    public void buscarDatosPorDNI(String dni) {
        if (dni.length() == 8) {
            try {
                String responseData = apiSunatService.consultarDocumento(dni);
                if (responseData != null) {
                    try {
                        JSONObject json = new JSONObject(responseData);
                        view.setDniField(dni);
                        view.setNombreField(json.optString("nombres", ""));
                        view.setApellidoField(json.optString("apellidoPaterno", "") + " " + json.optString("apellidoMaterno", ""));
                    } catch (JSONException e) {
                        mostrarError("Error al procesar la respuesta del servidor. Respuesta no válida.");
                        e.printStackTrace();
                    }
                } else {
                    mostrarError("No se encontraron datos");
                }
            } catch (Exception e) {
                mostrarError("Hubo un error al realizar la consulta");
                e.printStackTrace();
            }
        } else {
            mostrarError("El DNI debe tener 8 caracteres");
        }
    }

    private void mostrarError(String mensaje) {
        Notification.show(mensaje, 3000, Notification.Position.MIDDLE);
    }
}
