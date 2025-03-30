package pe.com.sammis.vale.vistas;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouterLink;

@CssImport("./themes/mi-tema/styles.css")
public class MainLayout extends AppLayout {


    public MainLayout() {
        createHeader();
        createSidebar();
    }

    private void createHeader() {
        DrawerToggle toggle = new DrawerToggle();
        H1 logo = new H1("VALE");
        logo.getStyle().set("margin", "0");

        HorizontalLayout header = new HorizontalLayout(toggle, logo);
        header.setWidthFull();
        header.setPadding(true);
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        addToNavbar(header);
    }

    private void createSidebar() {
        VerticalLayout sidebar = new VerticalLayout();
       /* sidebar.addClassName("custom-sidebar");*/
        sidebar.setSizeFull();
        sidebar.setPadding(true);
        sidebar.setSpacing(true);

        // Links de navegación
        RouterLink empleadoView = new RouterLink("Registro de empleados", EmpleadoCrudView.class);
        RouterLink tipoView = new RouterLink("Tipos de asistencia", TipoAsistenciaCrudView.class);
        RouterLink portalView = new RouterLink("MainView", MainView.class);
        RouterLink asistenciaView = new RouterLink("Registro de asistencias", AsistenciaCrudView.class);

        sidebar.add(empleadoView, portalView, tipoView,asistenciaView);

        addToDrawer(sidebar);
    }
}
