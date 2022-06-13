module org.dv.dv {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires itextpdf;
    requires GMapsFX;
    requires poi;
    requires java.desktop;
    requires java.sql;
    requires javafx.swing;
    requires mongo.java.driver;
    requires slf4j.api;
    requires logback.classic;
    requires json;
    requires org.apache.commons.io;

    opens org.dv.dv to javafx.fxml;
    exports org.dv.dv;
}