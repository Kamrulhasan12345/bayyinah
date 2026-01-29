module com.ks.bayyinah {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.ks.bayyinah to javafx.fxml;
    exports com.ks.bayyinah;
}
