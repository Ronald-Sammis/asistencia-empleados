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
        grid.getElement().getStyle().set("font-size", "12px");
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
        panel.setWidth("350px");  // Ajustar el ancho del panel

        // Recorrer cada empleado
        empleados.forEach(empleado -> {
            String textoEmpleado = empleado.getNombre() + " " + empleado.getApellido();

            // Crear ComboBox para cada empleado
            ComboBox<TipoAsistencia> select = new ComboBox<>();
            select.setItems(tiposAsistencia);
            select.setItemLabelGenerator(TipoAsistencia::getNombre);
            select.setPlaceholder("Seleccionar tipo de asistencia");
            select.setWidth("125px");

            // Crear el "card" usando un Div
            Div card = new Div();
            card.addClassName("empleado-card");
            card.setWidth("100%");
            // El card se ajustará al ancho del panel

            // Establecer el estilo directamente en el Div para el card
            card.getStyle()
                    .set("border", "1px solid #ddd")
                    .set("border-radius", "10px")
                    .set("margin-bottom", "5px");  // Reducir el margen entre los cards

            // Crear el layout dentro del card, pero con un VerticalLayout
            HorizontalLayout cardLayout = new HorizontalLayout();
            cardLayout.add(new Span(textoEmpleado));  // Nombre del empleado
            cardLayout.add(select);  // ComboBox de asistencia
            cardLayout.setAlignItems(Alignment.CENTER); // Centrar ambos elementos

            // Añadir un espaciado y padding mínimo dentro del card
            cardLayout.setSpacing(false);  // Reducir el espaciado dentro del card
            cardLayout.setPadding(true);  // Añadir padding mínimo

            // Añadir el layout al card
            card.add(cardLayout);

            // Agregar el card al panel
            panel.add(card);
        });

        // Agregar el panel a la vista o contenedor de tu UI
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

        Map<Asistencia, ComboBox<TipoAsistencia>> asistenciaMap = new HashMap<>();
        asistenciaGrid.addComponentColumn(asistencia -> {
            ComboBox<TipoAsistencia> select = new ComboBox<>();
            select.setItems(tipoAsistenciaRepository.findAll());
            select.setItemLabelGenerator(TipoAsistencia::getNombre);
            select.setValue(asistencia.getTipoAsistencia());
            asistenciaMap.put(asistencia, select);
            return select;
        }).setHeader("Asistencia");

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
        // Implementación para eliminar la asistencia
    }

    private void actualizarGrid() {
        grid.setItems(asistenciaRepository.findDistinctFechas());
    }
}
