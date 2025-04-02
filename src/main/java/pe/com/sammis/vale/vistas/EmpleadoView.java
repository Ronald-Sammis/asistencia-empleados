package pe.com.sammis.vale.vistas;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;
import pe.com.sammis.vale.controllers.EmpleadoController;
import pe.com.sammis.vale.models.Empleado;
import pe.com.sammis.vale.services.Interfaces.IEmpleadoService;
import pe.com.sammis.vale.services.implement.APISunatServiceImpl;

import java.util.List;

@Route(value = "empleado", layout = MainLayout.class)
public class EmpleadoView extends VerticalLayout {

    private final Grid<Empleado> grid = new Grid<>(Empleado.class);
    private final Button addButton = new Button("Nuevo", VaadinIcon.PLUS.create());
    private final TextField searchField = new TextField();
    private final TextField searchFieldDni = new TextField("Ingrese DNI");
    private final Dialog formDialog = new Dialog();
    private final TextField nombreField = new TextField("Nombre");
    private final TextField apellidoField = new TextField("Apellido");
    private final TextField dniField = new TextField("DNI");
    private final Button saveButton = new Button("Guardar", VaadinIcon.CHECK.create());
    private final Button cancelButton = new Button("Cancelar", VaadinIcon.CLOSE.create());
    private final EmpleadoController controller;

    @Autowired
    public EmpleadoView(IEmpleadoService empleadoService, APISunatServiceImpl apiSunatService) {
        addClassName("main-view");
        this.controller = new EmpleadoController(empleadoService, apiSunatService, this);

        add(new H2("Registro de empleados"));
        configureGrid();
        configureToolbar();
        configureForm();

        controller.updateList();
    }

    private void configureToolbar() {
        configureSearchField();
        configureAddButton();
        HorizontalLayout toolbar = new HorizontalLayout(addButton, searchField);
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN); // Separa los elementos a los extremos
        add(toolbar, grid);
    }

    private void configureSearchField() {
        searchField.setPlaceholder("Buscar...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);
        searchField.addValueChangeListener(event -> controller.filterList(event.getValue()));
    }

    private void configureAddButton() {
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.addClickListener(e -> controller.editarForm(new Empleado()));
    }

    private void configureGrid() {
        grid.setWidth("525px");
        grid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_COLUMN_BORDERS);
        grid.setColumns();
        grid.addColumn("dni").setAutoWidth(true);
        grid.addColumn(empleado -> empleado.getNombre() + " " + empleado.getApellido())
                .setHeader("Nombres").setAutoWidth(true);

        grid.addComponentColumn(this::createEditButton)
                .setHeader("Editar").setAutoWidth(true);
        grid.addComponentColumn(this::createDeleteButton)
                .setHeader("Eliminar").setAutoWidth(true);
    }

    private Button createEditButton(Empleado empleado) {
        Button editButton = new Button("Editar", VaadinIcon.PENCIL.create());
        editButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SMALL);
        // Pasa el objeto empleado al controlador
        editButton.addClickListener(e -> controller.editarEmpleado(empleado));
        return editButton;
    }


    // Método para crear el botón de eliminación en la vista
    private Button createDeleteButton(Empleado empleado) {
        Button deleteButton = new Button("Eliminar", VaadinIcon.TRASH.create());
        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SMALL);
        deleteButton.addClickListener(e -> controller.eliminarEmpleado(empleado));
        return deleteButton;
    }

    private void configureForm() {
        searchFieldDni.setClearButtonVisible(true);
        searchFieldDni.addValueChangeListener(e -> controller.buscarDatosPorDNI(e.getValue()));

        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(e -> controller.saveEmpleadoNuevo());
        cancelButton.addClickListener(e -> formDialog.close());

        VerticalLayout formLayout = new VerticalLayout(searchFieldDni, dniField, nombreField, apellidoField,
                new HorizontalLayout(saveButton, cancelButton));
        formDialog.add(formLayout);
    }




    public void updateList() {
        grid.setItems(controller.findAllEmpleados());
    }

    public void updateGrid(List<Empleado> empleados) {
        grid.setItems(empleados);
    }

    public void showNotification(String message, NotificationVariant variant) {
        Notification notification = Notification.show(message, 3000, Notification.Position.BOTTOM_CENTER);
        notification.addThemeVariants(variant);
    }

    public void openFormDialog() {
        formDialog.open();
    }

    public void closeFormDialog() {
        formDialog.close();
    }

    public void setFormFields(String nombre, String apellido, String dni) {
        nombreField.setValue(nombre != null ? nombre : "");
        apellidoField.setValue(apellido != null ? apellido : "");
        dniField.setValue(dni != null ? dni : "");
    }

    public String getNombre() {
        return nombreField.getValue();
    }

    public String getApellido() {
        return apellidoField.getValue();
    }

    public String getDni() {
        return dniField.getValue();
    }

    public void setDniField(String dni) {
        dniField.setValue(dni != null ? dni : "");
    }

    public void setNombreField(String nombre) {
        nombreField.setValue(nombre != null ? nombre : "");
    }

    public void setApellidoField(String apellido) {
        apellidoField.setValue(apellido != null ? apellido : "");
    }


}
