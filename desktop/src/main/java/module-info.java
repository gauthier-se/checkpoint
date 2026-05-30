module com.seyzeriat.desktop {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires java.desktop;

    opens com.seyzeriat.desktop to javafx.fxml;
    opens com.seyzeriat.desktop.controller to javafx.fxml;
    opens com.seyzeriat.desktop.dto to com.fasterxml.jackson.databind;

    exports com.seyzeriat.desktop;
    exports com.seyzeriat.desktop.controller;
    exports com.seyzeriat.desktop.dto;
    exports com.seyzeriat.desktop.service;
}
