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

    private final AsistenciaRepository asistenciaRepository;
    private final EmpleadoRepository empleadoRepository;
    private final TipoAsistenciaRepository tipoAsistenciaRepository;

    private final Grid<LocalDate> grid = new Grid<>(LocalDate.class, false);
    private final DatePicker fechaPicker = new DatePicker();
    private final Button nuevoButton = new Button("Nuevo");

    public AsistenciaCrudView(AsistenciaRepository asistenciaRepository,
                              EmpleadoRepository empleadoRepository,
                              TipoAsistenciaRepository tipoAsistenciaRepository) {
        addClassName("main-view");
        this.asistenciaRepository = asistenciaRepository;
        this.empleadoRepository = empleadoRepository;
        this.tipoAsistenciaRepository = tipoAsistenciaRepository;

        add(new H2("Registro de asistencias"));
        configurarBarraHerramientas();
        configurarGrid();
        add(grid);
        actualizarGrid();
    }

    private void configurarBarraHerramientas() {
        fechaPicker.setValue(LocalDate.now());
        fechaPicker.setWidth("125px");
        nuevoButton.addClickListener(e -> abrirModalRegistrar(fechaPicker.getValue()));
        nuevoButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        nuevoButton.setWidth("75px");
        add(new HorizontalLayout(fechaPicker, nuevoButton));
    }

    private void configurarGrid() {
        grid.setWidth("275px");
        grid.addColumn(fecha -> fecha.toString()).setHeader("Fecha")
                .setAutoWidth(true) // Puedes cambiar el valor según lo necesites
                .setFlexGrow(0); // Evita que la columna cambie de tamaño automáticamente

        grid.addComponentColumn(fecha -> {
            Button editarButton = new Button(VaadinIcon.EDIT.create());
            editarButton.addClickListener(e -> abrirModalEditar(fecha));
            editarButton.getStyle()
                    .set("background-color", "var(--lumo-warning-color)") // Fondo amarillo
                    .set("color", "black"); // Texto negro

            editarButton.setWidth("75px");

            Button eliminarButton = new Button(VaadinIcon.TRASH.create());
            eliminarButton.addClickListener(e -> eliminarAsistenciaPorFecha(fecha));
            eliminarButton.getStyle()
                    .set("background-color", "var(--lumo-error-color)") // Fondo amarillo
                    .set("color", "white"); // Texto negro
            eliminarButton.setWidth("75px");

            return new HorizontalLayout(editarButton, eliminarButton);
        }).setHeader("Acciones")
                .setAutoWidth(true);
        grid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_COLUMN_BORDERS);
    }




    private void abrirModalRegistrar(LocalDate fecha) {
        // Verificar si la fecha ya está registrada
        boolean fechaExistente = asistenciaRepository.findByFecha(fecha).size() > 0;
        if (fechaExistente) {
            Notification.show("Ya existe un registro de asistencia para esta fecha.", 3000, Notification.Position.BOTTOM_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

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
        modal.setWidth("400px");

        Grid<Empleado> empleadoGrid = new Grid<>(Empleado.class, false);
        empleadoGrid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_COLUMN_BORDERS);
        empleadoGrid.setItems(empleados);
        empleadoGrid.getStyle()
                .set("margin", "0")  // Elimina márgenes
                .set("padding", "0") // Elimina padding
                .set("width", "100%"); // Asegura que ocupe todo el ancho disponible

        Map<Empleado, ComboBox<TipoAsistencia>> asistenciaMap = new HashMap<>();

        empleadoGrid.addColumn(e -> e.getNombre() + " " + e.getApellido()).setHeader("Empleado").setAutoWidth(true);
        empleadoGrid.addComponentColumn(empleado -> {
                    ComboBox<TipoAsistencia> select = new ComboBox<>();
                    select.setItems(tiposAsistencia);
                    select.setItemLabelGenerator(TipoAsistencia::getNombre);
                    select.setWidth("100PX");
                    select.getStyle().set("border-radius", "30px");

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
        todosPuntualButton.getStyle().set("background", "#0eb90e");
        todosPuntualButton.getStyle().set("color", "white");

        Button sinRegistroButton = new Button("Quitar", e -> {
            tiposAsistencia.stream().filter(t -> "SIN_REGISTRO".equals(t.getNombre())).findFirst().ifPresent(tipo -> {
                asistenciaMap.values().forEach(select -> select.setValue(tipo));
            });
        });
        sinRegistroButton.getStyle().set("background", "#808080");
        sinRegistroButton.getStyle().set("color", "white");

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
        guardarButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cerrarButton = new Button("Cerrar", e -> modal.close());

        HorizontalLayout toolbarModal = new HorizontalLayout(todosPuntualButton, sinRegistroButton, guardarButton, cerrarButton);
        toolbarModal.setWidthFull();

        VerticalLayout modalLayout = new VerticalLayout(toolbarModal, empleadoGrid);
        modalLayout.setSizeFull();
        modalLayout.getStyle().set("padding", "0"); // Elimina el padding lateral del contenedor del Grid

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


}