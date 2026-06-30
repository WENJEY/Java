module com.example.lrtmap {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.lrtmap to javafx.fxml;
    exports com.example.lrtmap;
}