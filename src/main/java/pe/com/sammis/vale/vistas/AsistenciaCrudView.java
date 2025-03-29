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
        grid.addComponentColumn(fecha -> new HorizontalLayout(new Button("Eliminar", e -> eliminarAsistenciaPorFecha(fecha)))).setHeader("Acciones");
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
        modal.setWidth("600px");

        Grid<Empleado> empleadoGrid = new Grid<>(Empleado.class, false);
        empleadoGrid.setItems(empleados);
        Map<Empleado, ComboBox<TipoAsistencia>> asistenciaMap = new HashMap<>();

        empleadoGrid.addColumn(e -> e.getNombre() + " " + e.getApellido()).setHeader("Empleado").setWidth("150px");
        empleadoGrid.addComponentColumn(empleado -> {
            ComboBox<TipoAsistencia> select = new ComboBox<>();
            select.setItems(tiposAsistencia);
            select.setItemLabelGenerator(TipoAsistencia::getNombre);
            select.setWidth("130px");
            select.getStyle().set("border-radius", "30px");
            asistenciaMap.put(empleado, select);

            select.addValueChangeListener(event -> {
                TipoAsistencia seleccionado = event.getValue();
                if (seleccionado != null && seleccionado.getColorHex() != null) {
                    select.getStyle().set("background-color", seleccionado.getColorHex());
                    select.getStyle().set("color", getContrastingTextColor(seleccionado.getColorHex()));
                }
            });
            return select;
        }).setHeader("Tipo de Asistencia").setWidth("160px");

        Button todosPuntualButton = new Button("PUNTUAL", e -> {
            tiposAsistencia.stream().filter(t -> "PUNTUAL".equals(t.getNombre())).findFirst().ifPresent(tipo -> {
                asistenciaMap.values().forEach(select -> select.setValue(tipo));
            });
        });

        Button sinRegistroButton = new Button("SIN_REGISTRO", e -> {
            tiposAsistencia.stream().filter(t -> "SIN_REGISTRO".equals(t.getNombre())).findFirst().ifPresent(tipo -> {
                asistenciaMap.values().forEach(select -> select.setValue(tipo));
            });
        });

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

        HorizontalLayout toolbarModal = new HorizontalLayout(todosPuntualButton, sinRegistroButton, guardarButton, cerrarButton);
        toolbarModal.setWidthFull();

        VerticalLayout modalLayout = new VerticalLayout(toolbarModal, empleadoGrid);
        modalLayout.setSizeFull();
        modal.add(modalLayout);
        modal.open();
    }

    private void eliminarAsistenciaPorFecha(LocalDate fecha) {
        asistenciaRepository.deleteAll(asistenciaRepository.findByFecha(fecha));
        Notification.show("Asistencias eliminadas.");
        actualizarGrid();
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
}
