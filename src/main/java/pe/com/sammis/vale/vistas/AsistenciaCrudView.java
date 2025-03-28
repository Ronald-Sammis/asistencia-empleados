package pe.com.sammis.vale.vistas;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
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
import java.util.List;

@Route(value = "asistencia", layout = MainLayout.class)
@PageTitle("Gestión de Asistencias")
public class AsistenciaCrudView extends VerticalLayout {

    private final AsistenciaRepository asistenciaRepository;
    private final EmpleadoRepository empleadoRepository;
    private final TipoAsistenciaRepository tipoAsistenciaRepository;

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
        add(new HorizontalLayout(fechaPicker, nuevoButton));
    }

    private void configurarGrid() {
        grid.getElement().getStyle().set("font-size", "10px");
        grid.addColumn(LocalDate::toString).setHeader("Fecha");
        grid.addComponentColumn(fecha -> {
            Button editar = new Button(new Icon(VaadinIcon.EDIT), e -> abrirModalEdicion(fecha));
            Button eliminar = new Button(new Icon(VaadinIcon.TRASH), e -> eliminarAsistenciaPorFecha(fecha));
            return new HorizontalLayout(editar, eliminar);
        }).setHeader("Acciones");
    }

    private void abrirModal(LocalDate fecha) {
        if (fecha.isAfter(LocalDate.now())) {
            Notification.show("No se puede registrar asistencia para una fecha futura.", 3000, Notification.Position.BOTTOM_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        if (asistenciaRepository.existsByFecha(fecha)) {
            Notification.show("Ya existen asistencias registradas para esta fecha.", 3000, Notification.Position.BOTTOM_CENTER)
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

        // Ocultar el Grid
        grid.setVisible(false);

        // Crear un contenedor para todos los cards
        VerticalLayout panel = new VerticalLayout();
        panel.setSpacing(false); // Reducir espaciado entre los cards
        panel.setWidth("360px");  // Ajustar el ancho del panel

        // Recorrer cada empleado
        empleados.forEach(empleado -> {
            String textoEmpleado = empleado.getNombre() + " " + empleado.getApellido();

            // Crear ComboBox para cada empleado
            ComboBox<TipoAsistencia> select = new ComboBox<>();
            select.setItems(tiposAsistencia);  // Establecer los tipos de asistencia en el ComboBox
            select.setItemLabelGenerator(TipoAsistencia::getNombre);  // Mostrar solo el nombre de la asistencia
            select.setPlaceholder("Seleccionar tipo de asistencia");
            select.setWidth("125px");

            // Establecer tamaño de letra para el ComboBox
            select.getStyle().set("font-size", "10px");
            select.getElement().getStyle().set("font-size", "10px");  // Asegurar que el dropdown también tenga tamaño de letra 10px

            // Establecer valor por defecto con texto "SIN_REGISTRO"
            TipoAsistencia tipoAsistenciaDefault = new TipoAsistencia();
            tipoAsistenciaDefault.setNombre("SIN_REGISTRO");
            select.setValue(tipoAsistenciaDefault);  // Establecer el valor por defecto

            // Crear el "card" usando un Div
            Div card = new Div();
            card.addClassName("empleado-card");
            card.setWidth("100%");

            // Establecer el estilo directamente en el Div para el card
            card.getStyle()
                    .set("border", "1px solid #ddd")
                    .set("border-radius", "10px")
                    .set("margin-bottom", "5px")
                    .set("font-size", "10px");  // Establecer tamaño de letra a 10px en el card

            // Crear el layout dentro del card
            HorizontalLayout cardLayout = new HorizontalLayout();

            // Crear un Span con el nombre completo
            Span spanEmpleado = new Span(textoEmpleado);
            spanEmpleado.setWidth("200px");  // Establecer un ancho fijo para el Span
            spanEmpleado.getStyle().set("white-space", "nowrap");  // Evitar que el texto se divida en varias líneas

            // Añadir el Span y el ComboBox al layout
            cardLayout.add(spanEmpleado);  // Nombre del empleado
            cardLayout.add(select);  // ComboBox de asistencia
            cardLayout.setAlignItems(Alignment.CENTER); // Centrar ambos elementos

            // Añadir espaciado y padding mínimo dentro del card
            cardLayout.setSpacing(false);
            cardLayout.setPadding(true);

            // Añadir el layout al card
            card.add(cardLayout);

            // Cambiar el color del card y del texto según la selección del ComboBox
            select.addValueChangeListener(event -> {
                TipoAsistencia tipoAsistenciaSeleccionado = event.getValue();
                if (tipoAsistenciaSeleccionado != null) {
                    String colorHex = tipoAsistenciaSeleccionado.getColorHex(); // Obtener el color del tipo de asistencia
                    if (colorHex != null && !colorHex.isEmpty()) {
                        card.getStyle().set("background-color", colorHex);  // Cambiar el color del card

                        // Determinar el color de texto según el color de fondo
                        String textoColor = calcularColorTexto(colorHex);
                        spanEmpleado.getStyle().set("color", textoColor);  // Cambiar color del texto en el card
                        select.getStyle().set("color", textoColor);  // Cambiar color del texto en el ComboBox
                    } else {
                        // Asegurar un valor por defecto si el color no está definido
                        card.getStyle().set("background-color", "#FFFFFF");
                        spanEmpleado.getStyle().set("color", "#000000");  // Text color blanco por defecto
                        select.getStyle().set("color", "#000000");
                    }
                } else {
                    // Si no hay selección, usar el color blanco por defecto
                    card.getStyle().set("background-color", "#FFFFFF");
                    spanEmpleado.getStyle().set("color", "#000000");  // Text color blanco por defecto
                    select.getStyle().set("color", "#000000");
                }
            });

            // Agregar el card al panel
            panel.add(card);
        });

        // Agregar el panel a la vista
        add(panel);

        // Mostrar un botón para volver a la lista
        Button volverButton = new Button("Volver a la lista", e -> {
            remove(panel);  // Elimina el panel con los cards
            grid.setVisible(true);  // Vuelve a mostrar el Grid
        });

        add(volverButton);  // Añadir el botón de volver a la lista
    }

    private void abrirModalEdicion(LocalDate fecha) {
        List<Asistencia> asistencias = asistenciaRepository.findByFecha(fecha);
        if (asistencias.isEmpty()) {
            Notification.show("No hay asistencias registradas para esta fecha.");
            return;
        }

        Dialog modal = new Dialog();
        modal.setWidth("50vw");
        modal.setHeight("60vh");

        Grid<Asistencia> asistenciaGrid = new Grid<>(Asistencia.class, false);
        asistenciaGrid.setItems(asistencias);

        asistenciaGrid.addColumn(a -> a.getEmpleado().getNombre() + " " + a.getEmpleado().getApellido()).setHeader("Nombres");

        // Columna para seleccionar el tipo de asistencia
        asistenciaGrid.addComponentColumn(asistencia -> {
            ComboBox<TipoAsistencia> select = new ComboBox<>();
            select.setItems(tipoAsistenciaRepository.findAll());
            select.setItemLabelGenerator(TipoAsistencia::getNombre);
            select.setValue(asistencia.getTipoAsistencia());
            select.addValueChangeListener(event -> {
                asistencia.setTipoAsistencia(event.getValue());
            });
            return select;
        }).setHeader("Asistencia");

        Button guardarButton = new Button("Guardar", e -> {
            // Guardamos los cambios directamente desde los objetos Asistencia
            asistenciaRepository.saveAll(asistencias);
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
        List<Asistencia> asistencias = asistenciaRepository.findByFecha(fecha);
        if (!asistencias.isEmpty()) {
            asistenciaRepository.deleteAll(asistencias);
            Notification.show("Asistencias eliminadas.");
            actualizarGrid();
        }
    }

    private void actualizarGrid() {
        grid.setItems(asistenciaRepository.findDistinctFechas());
    }

    private String calcularColorTexto(String colorHex) {
        // Lógica para determinar si el fondo es claro u oscuro
        int r = Integer.parseInt(colorHex.substring(1, 3), 16);
        int g = Integer.parseInt(colorHex.substring(3, 5), 16);
        int b = Integer.parseInt(colorHex.substring(5, 7), 16);

        double luminosidad = 0.2126 * r + 0.7152 * g + 0.0722 * b;

        return luminosidad > 128 ? "#000000" : "#FFFFFF";  // Si la luminosidad es alta (claro), el texto será negro
    }
}
