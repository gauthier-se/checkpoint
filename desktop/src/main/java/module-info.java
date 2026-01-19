module com.seyzeriat.desktop {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.seyzeriat.desktop to javafx.fxml;
    exports com.seyzeriat.desktop;
}