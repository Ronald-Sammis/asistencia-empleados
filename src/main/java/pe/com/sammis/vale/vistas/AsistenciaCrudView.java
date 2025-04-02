package pe.com.sammis.vale.vistas;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.Lumo;
import com.vaadin.flow.theme.lumo.LumoUtility;
import pe.com.sammis.vale.models.Asistencia;
import pe.com.sammis.vale.models.Empleado;
import pe.com.sammis.vale.models.TipoAsistencia;
import pe.com.sammis.vale.repositories.AsistenciaRepository;
import pe.com.sammis.vale.repositories.EmpleadoRepository;
import pe.com.sammis.vale.repositories.TipoAsistenciaRepository;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Route(value = "asistencia", layout = MainLayout.class)
@PageTitle("Gestión de Asistencias")
public class AsistenciaCrudView extends VerticalLayout {

    private Dialog confirmDialog = new Dialog();
    private boolean notificationShown = false;
    private final AsistenciaRepository asistenciaRepository;
    private final EmpleadoRepository empleadoRepository;
    private final TipoAsistenciaRepository tipoAsistenciaRepository;

    private final Grid<LocalDate> grid = new Grid<>(LocalDate.class, false);
    private final DatePicker datePicker = new DatePicker();
    private final Button addButton = new Button("Nuevo");

    public AsistenciaCrudView(AsistenciaRepository asistenciaRepository,
                              EmpleadoRepository empleadoRepository,
                              TipoAsistenciaRepository tipoAsistenciaRepository) {
        addClassName("main-view");
        this.asistenciaRepository = asistenciaRepository;
        this.empleadoRepository = empleadoRepository;
        this.tipoAsistenciaRepository = tipoAsistenciaRepository;

        add(new H2("Registro de asistencias"));
        configureToolbar();
        configureGrid();
        add(grid);
        actualizarGrid();
    }

    private void configureToolbar() {
        datePicker.setValue(LocalDate.now());
        datePicker.setWidth("125px");
        addButton.addClickListener(e -> registerForm(datePicker.getValue()));
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.setWidth("75px");
        add(new HorizontalLayout(datePicker, addButton));
    }

    private void configureGrid() {
        configureGridProperties();
        addDataColumns();
        addActionColumns();
    }

    private void configureGridProperties() {
        grid.setWidth("275px");
        grid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_COLUMN_BORDERS);
        grid.setColumns();
    }

    private void addDataColumns() {
        grid.addColumn(fecha -> fecha.toString()).setHeader("Fecha")
                .setAutoWidth(true);

    }

    private void addActionColumns() {
        grid.addComponentColumn(this::createEditButton).setHeader("Editar").setAutoWidth(true);
        grid.addComponentColumn(this::createDeleteButton).setHeader("Eliminar").setAutoWidth(true);
    }

    private Button createEditButton(LocalDate fecha) {
        Button editButton = new Button(VaadinIcon.EDIT.create());
        editButton.addClickListener(event -> abrirModalEditar(fecha)); // Usamos la fecha de la asistencia
        styleButton(editButton, "var(--lumo-warning-color)", "black");
        return editButton;
    }

    private Button createDeleteButton(LocalDate fecha) {
        Button deleteButton = new Button(VaadinIcon.TRASH.create());
        deleteButton.addClickListener(event -> eliminarAsistenciaPorFecha(fecha)); // Usamos la fecha de la asistencia
        styleButton(deleteButton, "var(--lumo-error-color)", "white");
        return deleteButton;
    }


    private void styleButton(Button button, String bgColor, String textColor) {
        button.setWidth("75px");
        button.getStyle()
                .set("background-color", bgColor)
                .set("color", textColor);
    }


    private void registerForm(LocalDate fecha) {

        if (!validateDate(fecha)) return;
        if (!validateEmployeesAndAttendanceTypes()) return;


        List<TipoAsistencia> tiposAsistencia = tipoAsistenciaRepository.findAll();
        List<Empleado> empleados = empleadoRepository.findAll();


        Dialog modal = new Dialog();
        modal.setWidth("425px");

        Grid<Empleado> empleadoGrid = new Grid<>(Empleado.class, false);
        empleadoGrid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_COLUMN_BORDERS);
        empleadoGrid.setItems(empleados);

        Map<Empleado, ComboBox<TipoAsistencia>> asistenciaMap = new HashMap<>();

        empleadoGrid.addColumn(e -> e.getNombre() + " " + e.getApellido()).setHeader("Empleado").setAutoWidth(true);
        empleadoGrid.addComponentColumn(empleado -> {
                    ComboBox<TipoAsistencia> select = new ComboBox<>();
                    select.setItems(tiposAsistencia);
                    select.setItemLabelGenerator(TipoAsistencia::getNombre);
                    select.setWidth("140px");


                    TipoAsistencia sinRegistro = tiposAsistencia.stream()
                            .filter(t -> "SIN_REGISTRO".equals(t.getNombre()))
                            .findFirst()
                            .orElse(null);

                    if (sinRegistro != null) {
                        select.setValue(sinRegistro);
                        String colorHex = sinRegistro.getColorHex();
                        if (colorHex != null) {
                            select.getStyle().set("background-color", colorHex);
                            select.getStyle().set("color", getContrastingTextColor(colorHex));
                        }
                    }

                    List<Asistencia> asistencias = asistenciaRepository.findByFecha(fecha);
                    Asistencia asistenciaExistente = asistencias.stream()
                            .filter(a -> a.getEmpleado().equals(empleado))
                            .findFirst()
                            .orElse(null);

                    if (asistenciaExistente != null) {
                        select.setValue(asistenciaExistente.getTipoAsistencia());

                        String colorHex = asistenciaExistente.getTipoAsistencia().getColorHex();
                        if (colorHex != null) {
                            select.getStyle().set("background-color", colorHex);
                            select.getStyle().set("color", getContrastingTextColor(colorHex));
                        }
                    }

                    select.addValueChangeListener(event -> {
                        TipoAsistencia seleccionado = event.getValue();
                        if (seleccionado != null && seleccionado.getColorHex() != null) {
                            select.getStyle().set("background-color", seleccionado.getColorHex());
                            select.getStyle().set("color", getContrastingTextColor(seleccionado.getColorHex()));
                        }
                    });

                    asistenciaMap.put(empleado, select);
                    return select;
                }).setHeader("Tipo de Asistencia").setWidth("160px")
                .setAutoWidth(true);

        Button todosPuntualButton = new Button("Puntual", e -> {
            tiposAsistencia.stream().filter(t -> "PUNTUAL".equals(t.getNombre())).findFirst().ifPresent(tipo -> {
                asistenciaMap.values().forEach(select -> select.setValue(tipo));
            });
        });


        String colorHexPuntual = tiposAsistencia.stream()
                .filter(t -> "PUNTUAL".equals(t.getNombre()))
                .map(TipoAsistencia::getColorHex)
                .findFirst()
                .orElse("");

        todosPuntualButton.getStyle().set("background",colorHexPuntual).set("color",getContrastingTextColor(colorHexPuntual));


        Button noRecordButton  = new Button("Quitar", e -> {
            tiposAsistencia.stream().filter(t -> "SIN_REGISTRO".equals(t.getNombre())).findFirst().ifPresent(tipo -> {
                asistenciaMap.values().forEach(select -> select.setValue(tipo));
            });
        });
        noRecordButton.getStyle().set("background", "#808080");
        noRecordButton.getStyle().set("color", "white");

        Button saveButton = new Button("Guardar", e -> {
            asistenciaMap.forEach((empleado, comboBox) -> {
                TipoAsistencia tipoSeleccionado = comboBox.getValue();
                if (tipoSeleccionado != null) {
                    Asistencia asistencia = new Asistencia();
                    asistencia.setFecha(fecha);
                    asistencia.setEmpleado(empleado);
                    asistencia.setTipoAsistencia(tipoSeleccionado);
                    asistenciaRepository.save(asistencia);
                }
            });
            Notification.show("Asistencias registradas correctamente.");
            modal.close();
            actualizarGrid();
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button closeButton = new Button("Cerrar", e -> modal.close());

        HorizontalLayout toolbarModal = new HorizontalLayout(todosPuntualButton, noRecordButton, saveButton, closeButton);
        toolbarModal.setWidthFull();

        VerticalLayout modalLayout = new VerticalLayout(toolbarModal, empleadoGrid);
        modalLayout.setSizeFull();
        modalLayout.getStyle().set("padding", "0"); // Elimina el padding lateral del contenedor del Grid

        modal.add(modalLayout);
        modal.open();
    }





    private void eliminarAsistenciaPorFecha(LocalDate fecha) {
        /*asistenciaRepository.deleteAll(asistenciaRepository.findByFecha(fecha));
        Notification.show("Asistencias eliminadas.");
        actualizarGrid();*/


        confirmDialog.removeAll();
        confirmDialog.add("¿Está seguro de que desea eliminar esta asistencia:? " + fecha);

        Button confirmButton = new Button("Sí", event -> {
            asistenciaRepository.deleteAll(asistenciaRepository.findByFecha(fecha));
            updateList();
            confirmDialog.close();
            notificationShown = false; // Reset para permitir nuevas notificaciones
            Notification notification = Notification.show("Asistencia eliminado:" + fecha, 3000, Notification.Position.BOTTOM_CENTER);
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            notification.addOpenedChangeListener(e -> notificationShown = false);
        });
        confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("No", event -> confirmDialog.close());
        confirmDialog.add(new HorizontalLayout(confirmButton, cancelButton));
        confirmDialog.open();



    }

    private void actualizarGrid() {
        grid.setItems(asistenciaRepository.findDistinctFechas());
    }

    private String getContrastingTextColor(String colorHex) {
        int r = Integer.parseInt(colorHex.substring(1, 3), 16);
        int g = Integer.parseInt(colorHex.substring(3, 5), 16);
        int b = Integer.parseInt(colorHex.substring(5, 7), 16);
        return (r * 0.299 + g * 0.587 + b * 0.114) > 128 ? "black" : "white";
    }

    private void abrirModalEditar(LocalDate fecha) {
        List<Asistencia> asistencias = asistenciaRepository.findByFecha(fecha);
        if (asistencias.isEmpty()) {
            Notification.show("No hay registros de asistencia para esta fecha.", 3000, Notification.Position.BOTTOM_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }

        Dialog modal = new Dialog();
        modal.setWidth("400px");

        Grid<Asistencia> asistenciaGrid = new Grid<>(Asistencia.class, false);
        asistenciaGrid.setItems(asistencias);
        Map<Asistencia, ComboBox<TipoAsistencia>> asistenciaMap = new HashMap<>();

        asistenciaGrid.addColumn(a -> a.getEmpleado().getNombre() + " " + a.getEmpleado().getApellido())
                .setHeader("Empleado").setWidth("150px");

        asistenciaGrid.addComponentColumn(asistencia -> {
            ComboBox<TipoAsistencia> select = new ComboBox<>();
            List<TipoAsistencia> tiposAsistencia = tipoAsistenciaRepository.findAll();
            select.setItems(tiposAsistencia);
            select.setItemLabelGenerator(TipoAsistencia::getNombre);
            select.setWidth("130px");
            select.getStyle().set("border-radius", "30px");

            // Precarga el valor de tipo de asistencia
            select.setValue(asistencia.getTipoAsistencia());

            // Aplicar el color de fondo si el tipo de asistencia tiene un color
            TipoAsistencia tipoSeleccionado = asistencia.getTipoAsistencia();
            if (tipoSeleccionado != null && tipoSeleccionado.getColorHex() != null) {
                select.getStyle().set("background-color", tipoSeleccionado.getColorHex());
                select.getStyle().set("color", getContrastingTextColor(tipoSeleccionado.getColorHex()));
            }

            select.addValueChangeListener(event -> {
                TipoAsistencia seleccionado = event.getValue();
                if (seleccionado != null && seleccionado.getColorHex() != null) {
                    select.getStyle().set("background-color", seleccionado.getColorHex());
                    select.getStyle().set("color", getContrastingTextColor(seleccionado.getColorHex()));
                }
            });

            asistenciaMap.put(asistencia, select);
            return select;
        }).setHeader("Tipo de Asistencia").setWidth("160px");

        Button guardarButton = new Button("Guardar", e -> {
            asistenciaMap.forEach((asistencia, comboBox) -> {
                TipoAsistencia tipoSeleccionado = comboBox.getValue();
                if (tipoSeleccionado != null) {
                    asistencia.setTipoAsistencia(tipoSeleccionado);
                    asistenciaRepository.save(asistencia);
                }
            });
            Notification.show("Asistencias actualizadas correctamente.");
            modal.close();
            actualizarGrid();
        });
        guardarButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cerrarButton = new Button("Cerrar", e -> modal.close());

        HorizontalLayout toolbarModal = new HorizontalLayout(guardarButton, cerrarButton);
        toolbarModal.setWidthFull();

        VerticalLayout modalLayout = new VerticalLayout(toolbarModal, asistenciaGrid);
        modalLayout.setSizeFull();
        modal.add(modalLayout);
        modal.open();
    }

    private void updateList() {
        grid.setItems(asistenciaRepository.findDistinctFechas());
    }

    private boolean validateDate(LocalDate fecha) {
        // Validación si la fecha ya está registrada
        if (asistenciaRepository.findByFecha(fecha).size() > 0) {
            showErrorNotification("Ya existe un registro de asistencia para esta fecha.");
            return false;
        }

        // Validación si la fecha es futura
        if (fecha.isAfter(LocalDate.now())) {
            showErrorNotification("No se puede registrar asistencia para una fecha futura.");
            return false;
        }



        return true;
    }

    private boolean validateEmployeesAndAttendanceTypes() {
        List<TipoAsistencia> tiposAsistencia = tipoAsistenciaRepository.findAll();
        List<Empleado> empleados = empleadoRepository.findAll();

        if (tiposAsistencia.isEmpty() || empleados.isEmpty()) {
            showErrorNotification("Debe existir al menos un empleado y un tipo de asistencia para registrar asistencia.");
            return false;
        }
        return true;
    }

    private void showErrorNotification(String message) {
        Notification.show(message, 3000, Notification.Position.BOTTOM_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }



}