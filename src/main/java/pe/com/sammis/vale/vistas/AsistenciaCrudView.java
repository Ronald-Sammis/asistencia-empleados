package pe.com.sammis.vale.vistas;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import pe.com.sammis.vale.models.Asistencia;
import pe.com.sammis.vale.models.Empleado;
import pe.com.sammis.vale.models.TipoAsistencia;
import pe.com.sammis.vale.repositories.AsistenciaRepository;
import pe.com.sammis.vale.repositories.EmpleadoRepository;
import pe.com.sammis.vale.repositories.TipoAsistenciaRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Route(value = "asistencia", layout = MainLayout.class)
@PageTitle("Gestión de Asistencias")
public class AsistenciaCrudView extends VerticalLayout {

    private final AsistenciaRepository asistenciaRepository;
    private final EmpleadoRepository empleadoRepository;
    private final TipoAsistenciaRepository tipoAsistenciaRepository;
    private Dialog confirmDialog = new Dialog();
    private boolean notificationShown = false;

    private final Grid<LocalDate> grid = new Grid<>(LocalDate.class, false);
    private final DatePicker fechaPicker = new DatePicker();
    private final Button nuevoButton = new Button(new Icon(VaadinIcon.PLUS_CIRCLE));

    public AsistenciaCrudView(AsistenciaRepository asistenciaRepository,
                              EmpleadoRepository empleadoRepository,
                              TipoAsistenciaRepository tipoAsistenciaRepository) {
        addClassName("main-view");
        this.asistenciaRepository = asistenciaRepository;
        this.empleadoRepository = empleadoRepository;
        this.tipoAsistenciaRepository = tipoAsistenciaRepository;

        add(new H1("Asistencia"));
        configurarBarraHerramientas();
        configurarGrid();
        add(grid);
        actualizarGrid();
    }

    private void configurarBarraHerramientas() {
        fechaPicker.setValue(LocalDate.now());
        nuevoButton.addClickListener(e -> abrirModal(fechaPicker.getValue()));
        HorizontalLayout toolbar = new HorizontalLayout(fechaPicker, nuevoButton);
        toolbar.setWidthFull();
        add(toolbar);
    }

    private void configurarGrid() {
        grid.getElement().getStyle().set("font-size", "12px");
        grid.addColumn(LocalDate::toString).setHeader("Fecha");
        grid.addComponentColumn(fecha -> {
            Button editar = new Button(new Icon(VaadinIcon.EDIT), e -> abrirModalEdicion(fecha));
            Button eliminar = new Button(new Icon(VaadinIcon.TRASH), e -> eliminarAsistenciaPorFecha(fecha));
            return new HorizontalLayout(editar, eliminar);
        }).setHeader("Acciones");
    }

    private void abrirModal(LocalDate fecha) {
        // ✅ Validación: No permitir registrar una fecha futura
        if (fecha.isAfter(LocalDate.now())) {
            Notification notificacion=Notification.show("No se puede registrar asistencia para una fecha futura.",3000, Notification.Position.BOTTOM_CENTER);
            notificacion.addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        // ✅ Validación: No permitir registrar una fecha ya tratada
        if (asistenciaRepository.existsByFecha(fecha)) {
            Notification notificacion=Notification.show("Ya existen asistencias registradas para esta fecha.",3000, Notification.Position.BOTTOM_CENTER);
            notificacion.addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        List<TipoAsistencia> tiposAsistencia = tipoAsistenciaRepository.findAll();
        List<Empleado> empleados = empleadoRepository.findAll();

        if (tiposAsistencia.isEmpty() || empleados.isEmpty()) {
            Notification notificacion=Notification.show("Debe existir al menos un empleado y un tipo de asistencia para registrar asistencia.",3000, Notification.Position.BOTTOM_CENTER);
            notificacion.addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        Dialog modal = new Dialog();
        modal.setWidth("60vw");
        modal.setHeight("70vh");

        Grid<Empleado> empleadosGrid = new Grid<>(Empleado.class, false);
        empleadosGrid.setItems(empleados);

        // ✅ Columna de FECHA basada en el DatePicker seleccionado
        empleadosGrid.addColumn(e -> fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .setHeader("Fecha");

        // ✅ Columna de ID del empleado
        empleadosGrid.addColumn(Empleado::getId).setHeader("ID");

        // ✅ Columna combinada de Nombres (Nombre + Apellido)
        empleadosGrid.addColumn(e -> e.getNombre() + " " + e.getApellido()).setHeader("Nombres");

        Map<Empleado, ComboBox<TipoAsistencia>> asistenciaMap = new HashMap<>();
        empleadosGrid.addComponentColumn(empleado -> {
            ComboBox<TipoAsistencia> select = new ComboBox<>();
            select.setItems(tiposAsistencia);
            select.setItemLabelGenerator(TipoAsistencia::getNombre);
            asistenciaMap.put(empleado, select);
            return select;
        }).setHeader("Asistencia");

        // ✅ Barra de herramientas en la parte superior del modal
        Button guardarButton = new Button("Guardar", e -> {
            empleados.forEach(empleado -> {
                TipoAsistencia tipoSeleccionado = asistenciaMap.get(empleado).getValue();
                if (tipoSeleccionado != null) {
                    Asistencia nuevaAsistencia = new Asistencia();
                    nuevaAsistencia.setEmpleado(empleado);
                    nuevaAsistencia.setFecha(fecha); // ✅ Se guarda correctamente la fecha
                    nuevaAsistencia.setTipoAsistencia(tipoSeleccionado);
                    asistenciaRepository.save(nuevaAsistencia);
                }
            });
            modal.close();
            actualizarGrid();
        });
        guardarButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cerrarButton = new Button("Cerrar", e -> modal.close());

        HorizontalLayout toolbarModal = new HorizontalLayout(guardarButton, cerrarButton);
        toolbarModal.setWidthFull();

        VerticalLayout modalLayout = new VerticalLayout(toolbarModal, empleadosGrid);
        modal.add(modalLayout);
        modal.open();
    }

    private void abrirModalEdicion(LocalDate fecha) {
        List<Asistencia> asistencias = asistenciaRepository.findByFecha(fecha);
        if (asistencias.isEmpty()) {
            Notification.show("No hay asistencias registradas para esta fecha.");
            return;
        }

        Dialog modal = new Dialog();
        modal.setWidth("60vw");
        modal.setHeight("70vh");

        Grid<Asistencia> asistenciaGrid = new Grid<>(Asistencia.class, false);
        asistenciaGrid.setItems(asistencias);
        asistenciaGrid.addColumn(a -> a.getEmpleado().getNombre()+" "+a.getEmpleado().getApellido()).setHeader("Nombres");


        Map<Asistencia, ComboBox<TipoAsistencia>> asistenciaMap = new HashMap<>();
        asistenciaGrid.addComponentColumn(asistencia -> {
            ComboBox<TipoAsistencia> select = new ComboBox<>();
            select.setItems(tipoAsistenciaRepository.findAll());
            select.setItemLabelGenerator(TipoAsistencia::getNombre);
            select.setValue(asistencia.getTipoAsistencia());
            asistenciaMap.put(asistencia, select);
            return select;
        }).setHeader("Asistencia");

        // ✅ Barra de herramientas en la parte superior del modal de edición
        Button guardarButton = new Button("Guardar", e -> {
            asistenciaMap.forEach((asistencia, select) -> {
                asistencia.setTipoAsistencia(select.getValue());
                asistenciaRepository.save(asistencia);
            });
            modal.close();
            actualizarGrid();
            Notification.show("Asistencias actualizadas correctamente.");
        });

        Button cerrarButton = new Button("Cerrar", e -> modal.close());

        HorizontalLayout toolbarModal = new HorizontalLayout(guardarButton, cerrarButton);
        toolbarModal.setWidthFull();

        VerticalLayout modalLayout = new VerticalLayout(toolbarModal, asistenciaGrid);
        modal.add(modalLayout);
        modal.open();
    }

    private void eliminarAsistenciaPorFecha(LocalDate fecha) {


        confirmDialog.removeAll();
        confirmDialog.add("¿Está seguro de que desea eliminar este registro de asistencias del: "+fecha+"?");

        Button confirmButton = new Button("Sí", event -> {

            asistenciaRepository.deleteAll(asistenciaRepository.findByFecha(fecha));
            actualizarGrid();
            confirmDialog.close();
            notificationShown = false; // Reset para permitir nuevas notificaciones
            Notification notification = Notification.show("Registro de asistencias del: " + fecha + " eliminado", 3000, Notification.Position.BOTTOM_CENTER);
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            notification.addOpenedChangeListener(e -> notificationShown = false);
        });
        confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("No", event -> confirmDialog.close());
        confirmDialog.add(new HorizontalLayout(confirmButton, cancelButton));
        confirmDialog.open();
    }

    private void actualizarGrid() {
        List<LocalDate> fechas = asistenciaRepository.findDistinctFechas();
        grid.setItems(fechas);
    }
}
