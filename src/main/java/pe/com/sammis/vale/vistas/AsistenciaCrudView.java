package pe.com.sammis.vale.vistas;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Route(value = "asistencia", layout = MainLayout.class)
@PageTitle("Gestión de Asistencias")
public class AsistenciaCrudView extends VerticalLayout {

    private final AsistenciaRepository asistenciaRepository;
    private final EmpleadoRepository empleadoRepository;
    private final TipoAsistenciaRepository tipoAsistenciaRepository;

    private final Grid<LocalDate> grid = new Grid<>(LocalDate.class, false);
    private final DatePicker fechaPicker = new DatePicker();
    private final Button nuevoButton = new Button("Nuevo Registro");

    public AsistenciaCrudView(AsistenciaRepository asistenciaRepository,
                              EmpleadoRepository empleadoRepository,
                              TipoAsistenciaRepository tipoAsistenciaRepository) {
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
        add(new HorizontalLayout(fechaPicker, nuevoButton));
    }

    private void configurarGrid() {
        grid.addColumn(fecha -> fecha.toString()).setHeader("Fecha");
        grid.addComponentColumn(fecha -> {
            Button eliminar = new Button("Eliminar", e -> eliminarAsistenciaPorFecha(fecha));
            return new HorizontalLayout(eliminar);
        }).setHeader("Acciones");
    }

    private void abrirModal(LocalDate fecha) {
        if (fecha.isAfter(LocalDate.now())) {
            Notification.show("No se puede registrar asistencia para una fecha futura.", 3000, Notification.Position.BOTTOM_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        List<TipoAsistencia> tiposAsistencia = tipoAsistenciaRepository.findAll();
        List<Empleado> empleados = empleadoRepository.findAll();

        if (tiposAsistencia.isEmpty() || empleados.isEmpty()) {
            Notification.show("Debe existir al menos un empleado y un tipo de asistencia para registrar asistencia.", 3000, Notification.Position.BOTTOM_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        Dialog modal = new Dialog();
        modal.setWidth("600px"); // Asegurar un ancho adecuado
        modal.setMinWidth("400px"); // Evitar que sea demasiado angosto
        modal.setHeight("auto"); // Que crezca según el contenido

        Grid<Empleado> empleadoGrid = new Grid<>(Empleado.class, false);
        empleadoGrid.setItems(empleados);
        empleadoGrid.setWidthFull(); // Ocupar todo el ancho disponible

        Map<Empleado, ComboBox<TipoAsistencia>> asistenciaMap = new HashMap<>();

        empleadoGrid.addColumn(e -> e.getNombre() + " " + e.getApellido())
                .setHeader("Empleado")
                .setAutoWidth(true)
                .setFlexGrow(1); // Permitir que crezca dinámicamente

        empleadoGrid.addComponentColumn(empleado -> {
            ComboBox<TipoAsistencia> select = new ComboBox<>();
            select.setItems(tiposAsistencia);
            select.setItemLabelGenerator(TipoAsistencia::getNombre);
            asistenciaMap.put(empleado, select);
            return select;
        }).setHeader("Tipo de Asistencia").setAutoWidth(true).setFlexGrow(1);

        Button guardarButton = new Button("Guardar", e -> {
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

        Button cerrarButton = new Button("Cerrar", e -> modal.close());

        HorizontalLayout toolbarModal = new HorizontalLayout(guardarButton, cerrarButton);
        toolbarModal.setWidthFull();
        toolbarModal.setJustifyContentMode(JustifyContentMode.CENTER);

        VerticalLayout modalLayout = new VerticalLayout(toolbarModal, empleadoGrid);
        modalLayout.setSizeFull();
        modal.add(modalLayout);

        modal.open();
    }








    private void eliminarAsistenciaPorFecha(LocalDate fecha) {
        List<Asistencia> asistencias = asistenciaRepository.findByFecha(fecha);
        asistenciaRepository.deleteAll(asistencias);
        Notification.show("Asistencias eliminadas.");
        actualizarGrid();
    }

    private void actualizarGrid() {
        grid.setItems(asistenciaRepository.findDistinctFechas());
    }
}
