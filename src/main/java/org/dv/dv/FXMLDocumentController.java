/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dv.dv;

import com.itextpdf.text.*;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.lynden.gmapsfx.GoogleMapView;
import com.lynden.gmapsfx.MapComponentInitializedListener;
import com.lynden.gmapsfx.javascript.object.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.*;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.TextField;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Pair;
import javafx.geometry.Insets;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.util.List;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.embed.swing.SwingFXUtils;
/**
 * http://java-buddy.blogspot.com/2013/03/javafx-interaction-between-table-and.html
 * http://stackoverflow.com/questions/24652043/one-obvservablelist-for-all-visualisations-in-javafx
 * http://java-buddy.blogspot.com/2012/04/create-and-update-linechart-from.html
 *
 *
 */
public class FXMLDocumentController implements Initializable, MapComponentInitializedListener {

    @FXML public TextField user_, ps_;
    @FXML public Pane cover;
    
    public void login(){
        if(user_.getText().equals("Admin") && ps_.getText().equals("Admin")){
            cover.setVisible(false);
        }else{
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Authentication error");
                    alert.setHeaderText("Authentication error");
                    alert.setContentText("Wrong username or password, try again...");
                    alert.showAndWait();
        }
    }
    
    private Connection conn;
    private Statement stmt;
    @FXML
    private TableView result_table;
    @FXML
    private ComboBox table_combo_box;
    @FXML
    private Label left_status;

    class ConnectThread extends Thread {

        String userName;
        String password;
        String serverName;
        String dbName;

        public ConnectThread(String userName, String password, String serverName, String dbName) {
            this.userName = userName;
            this.password = password;
            this.serverName = serverName;
            this.dbName = dbName;
        }

        @Override
        public void run() {
            try {
                System.out.println("CONNECTING TO DB");
                Class.forName("com.mysql.cj.jdbc.Driver");
                conn = DriverManager.getConnection("jdbc:mysql://" + this.serverName + "/" + this.dbName + "?allowPublicKeyRetrieval=true&useSSL=false", this.userName, this.password);
                stmt = conn.createStatement();
                System.out.println("Connected");
                SetTableListNames();
                System.out.println("SET TABLE LIST NAMES DONE");
                buildData("Select * from " + table_combo_box.getItems().get(0));
                //left_status.setText("");

            } catch (ClassNotFoundException | SQLException e) {
                System.out.println(e);
                System.out.println("Can't connect " + e.toString());
            }
        }
    }


    private String mysql_db_user = "root";
    private String mysql_db_pass = "password";
    private String mysql_db_addr = "localhost:3306";
    private String mysql_db_name = "dv";

    private MongoConnector mongoConnector;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        System.out.println("INitIALIZE of MAP");

        this.mapView = new GoogleMapView("en-US", "");
        this.map = new GoogleMap();
        mapView.addMapInializedListener(this);
        ConnectThread c = new ConnectThread(mysql_db_user, mysql_db_pass, mysql_db_addr, mysql_db_name);
        c.start();
        result_table.setItems(data);
        this.setChartChooser();
    }

    static class MysqlConn {

        public String mysql_db_user;
        public String mysql_db_pass;
        public String mysql_db_addr;
        public String mysql_db_name;

        public MysqlConn(String mysql_db_user, String mysql_db_pass, String mysql_db_addr, String mysql_db_name) {
            this.mysql_db_user = mysql_db_user;
            this.mysql_db_pass = mysql_db_pass;
            this.mysql_db_addr = mysql_db_addr;
            this.mysql_db_name = mysql_db_name;
        }

        @Override
        public String toString() {
            return "MysqlConn{" + "mysql_db_user=" + mysql_db_user + ", mysql_db_pass=" + mysql_db_pass + ", mysql_db_addr=" + mysql_db_addr + ", mysql_db_name=" + mysql_db_name + '}';
        }

    }
    MysqlConn mysqldata;

    public void openConnectionDialog() {

        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Connect to database");
        ButtonType loginButtonType = new ButtonType("Connect", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 10, 10, 10));
        TextField username = new TextField();
        username.setPromptText("Username");
        PasswordField password = new PasswordField();
        password.setPromptText("Password");
        grid.add(new Label("Username:"), 0, 0);
        grid.add(username, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(password, 1, 1);
        TextField db_name = new TextField();
        db_name.setPromptText("Database name");
        TextField db_address = new TextField();
        db_address.setPromptText("Database address");
        grid.add(new Label("Database name:"), 0, 2);
        grid.add(db_name, 1, 2);
        grid.add(new Label("Database address:"), 0, 3);
        grid.add(db_address, 1, 3);
        Node loginButton = dialog.getDialogPane().lookupButton(loginButtonType);
        loginButton.setDisable(true);
        username.textProperty().addListener((observable, oldValue, newValue) -> {
            loginButton.setDisable(newValue.trim().isEmpty());
        });
        dialog.getDialogPane().setContent(grid);
        Platform.runLater(() -> username.requestFocus());
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                mysqldata = new MysqlConn(username.getText(), password.getText(), db_address.getText(), db_name.getText());
                return new Pair<>("Data", "Data");
            }
            return null;
        });

        Optional<Pair<String, String>> result = dialog.showAndWait();

        result.ifPresent(usernamePassword -> {
            System.out.println(mysqldata.toString());
            System.out.println("Username=" + usernamePassword.getKey() + ", Password=" + usernamePassword.getValue());

            ConnectThread c
                    = new ConnectThread(mysqldata.mysql_db_user,
                            mysqldata.mysql_db_pass,
                            mysqldata.mysql_db_addr,
                            mysqldata.mysql_db_name);
            c.start();
        });

    }

    @FXML
    private GoogleMapView mapView;
    private GoogleMap map;

    @FXML
    private ComboBox chartPicker;
    @FXML
    private AnchorPane pieChartSettings;
    @FXML
    private AnchorPane lineChartSettings;
    @FXML
    private AnchorPane areaChartSettings;
    @FXML
    private AnchorPane mapViewSettings;
    @FXML
    public PieChart pie_chart;
    @FXML
    public AreaChart area_chart;
    @FXML
    public LineChart line_chart;

    @FXML
    public AnchorPane mapViewAnchorPane;
    @FXML
    public ComboBox locationChoser;

    public void setChartChooser() {
        dropVisibility();
        ObservableList<String> options = FXCollections.observableArrayList();
        options.add("Pie chart");
        options.add("Line chart");
        options.add("Area chart");
        options.add("Location map");
        chartPicker.setItems(options);
        chartPicker.valueProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue ov, String t, String t1) {
                System.out.println(t1);
                dropVisibility();
                switch (t1) {
                    case "Pie chart":
                        pieChartSettings.setVisible(true);
                        pie_chart.setVisible(true);
                        break;
                    case "Line chart":
                        lineChartSettings.setVisible(true);
                        line_chart.setVisible(true);
                        break;
                    case "Area chart":
                        areaChartSettings.setVisible(true);
                        area_chart.setVisible(true);
                        break;
                    case "Location map":
                        System.out.println("LOCATION MAP HERE");
                        mapViewAnchorPane.setVisible(true);
                        mapViewSettings.setVisible(true);
                        break;
                    default:
                        break;
                }
            }
        });
    }

    public void dropVisibility() {
        pieChartSettings.setVisible(false);
        lineChartSettings.setVisible(false);
        areaChartSettings.setVisible(false);
        pie_chart.setVisible(false);
        area_chart.setVisible(false);
        line_chart.setVisible(false);
        mapViewAnchorPane.setVisible(false);
        mapViewSettings.setVisible(false);
        locationChoser.setVisible(false);
    }

    class Cont {

        List<String> data;

        public Cont(List<String> data) {
            this.data = data;
        }
    }

    public void SetTableListNames() {
        try {
            ObservableList<String> options = FXCollections.observableArrayList();
            DatabaseMetaData md = conn.getMetaData();
            ResultSet rs = md.getTables(conn.getCatalog(), null, "%", null);
            while (rs.next()) {
                String n = rs.getString(3);
                options.add(n);
                System.out.println(n);
            }
            this.table_combo_box.setItems(options);
            this.Pie_chart_data.setItems(options);
        } catch (Exception e) {
            System.out.println(e);
            System.out.println("Error on Building Data");
        }

        table_combo_box.valueProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue ov, String t, String t1) {
                System.out.println(t1);
                buildData("Select * from " + t1);
            }
        });
    }

    private ObservableList<ObservableList> data = FXCollections.observableArrayList();
    public List<String> headers;

    public void buildData(String sql) {
        result_table.getColumns().clear();
        result_table.getItems().clear();
        data.clear();
        data.removeAll(data);
        chart_options.removeAll(chart_options);
        chart_options.clear();

        try {

            ResultSet rs = stmt.executeQuery(sql); //когда будет соединение с базой
            headers = new ArrayList<>();
            System.out.println("HEADERS INITIALIZED");
            for (int i = 0; i < rs.getMetaData().getColumnCount(); i++) {
                final int j = i;
                String header_name = rs.getMetaData().getColumnName(i + 1);
                TableColumn col = new TableColumn(header_name); //когда будет соединение с базой

                col.setCellValueFactory(new Callback<CellDataFeatures<ObservableList, String>, ObservableValue<String>>() {
                    public ObservableValue<String> call(CellDataFeatures<ObservableList, String> param) {
                        return new SimpleStringProperty(param.getValue().get(j) != null ? param.getValue().get(j).toString() : "NULL");
                    }
                });

                result_table.getColumns().addAll(col);
                headers.add(header_name);
                chart_options.add(header_name);
                System.out.println("Column [" + i + "] ");
            }
            reload_chart_combo();
            while (rs.next()) {
                ObservableList<String> row = FXCollections.observableArrayList();
                for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                    row.add(rs.getString(i));
                }
                System.out.println("Row [1] added " + row);
                data.add(row);
            }

            if (result_table.getItems() == null) {
                result_table.setItems(data);
            }
        } catch (Exception e) {
            System.out.println(e);
            System.out.println("Error on Building Data " + e.toString());
        }
    }

    @FXML
    private ComboBox Pie_chart_header;
    @FXML
    private ComboBox Pie_chart_data;

    @FXML
    private ComboBox area_chart_header;
    @FXML
    private ComboBox area_chart_data;
    @FXML
    private ComboBox area_chart_legend;
    @FXML
    private ComboBox area_chart_group;

    @FXML
    private ComboBox line_chart_header;
    @FXML
    private ComboBox line_chart_data;
    @FXML
    private ComboBox line_chart_legend;
    @FXML
    private ComboBox line_chart_group;

    @FXML
    private ComboBox Map_view_header;
    @FXML
    private ComboBox Map_view_data;

    ObservableList<String> chart_options = FXCollections.observableArrayList();

    public void reload_chart_combo() {
        Pie_chart_header.setItems(chart_options);
        Pie_chart_data.setItems(chart_options);

        area_chart_header.setItems(chart_options);
        area_chart_data.setItems(chart_options);
        area_chart_legend.setItems(chart_options);
        area_chart_group.setItems(chart_options);

        line_chart_header.setItems(chart_options);
        line_chart_data.setItems(chart_options);
        line_chart_legend.setItems(chart_options);
        line_chart_group.setItems(chart_options);

        Map_view_header.setItems(chart_options);
        Map_view_data.setItems(chart_options);
    }

    public boolean isFound(String name, List<String> names) {
        for (String inames : names) {
            if (name.equals(inames)) {
                return true;
            }
        }
        return false;
    }

    class MapLocation {

        public String name;
        public Marker marker;
        public LatLong latLong;
        public InfoWindow iw;

        public MapLocation(String name, double Lat, double Long, String addInfo) {
            this.name = name;
            this.latLong = new LatLong(Lat, Long);
            this.marker = new Marker(new MarkerOptions().position(latLong));
            this.iw = new InfoWindow(new InfoWindowOptions().content("<h1>" + name + "</h1>" + addInfo));

        }

        public void OpenMarker() {
            iw.open(map, this.marker);
            map.setCenter(this.latLong);
            map.setZoom(14);
        }

        public void CloseMarker() {
            iw.close();
        }
    }



    @Override
    public void mapInitialized() {
        System.out.println("MAP INitiALized");
        listMapLocations = new ArrayList<>();
        MapOptions mapOptions = new MapOptions();
        try {
        mapOptions.center(new LatLong(46.5507224, 54.3167911))
                .overviewMapControl(false)
                .panControl(false)
                .rotateControl(false)
                .scaleControl(false)
                .streetViewControl(false)
                .zoomControl(false)
                .zoom(5);

        }catch (Exception e){
            System.out.println("EXCEPTION in map options " + e);
        }

        map = mapView.createMap(mapOptions);


        locationChoser.valueProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue ov, String t, String t1) {
                System.out.println(t1);
                for (MapLocation mpl : listMapLocations) {
                    System.out.println("Closing marker " + mpl.name);
                    mpl.CloseMarker();
                    if (t1.equals(mpl.name)) {
                        System.out.println("Open marker " + mpl.name);
                        mpl.OpenMarker();
                    }
                }
            }
        });
    }
    List<MapLocation> listMapLocations;

    public void reload_map_view() {
        int header = Map_view_header.getSelectionModel().getSelectedIndex();
        int datalocal = Map_view_data.getSelectionModel().getSelectedIndex();
        System.out.println("DATA OF MAP " + datalocal);

        locationChoser.setItems(null);

        //remove prev markers
        try{
            for (MapLocation mpl : listMapLocations) {
                map.removeMarker(mpl.marker);
            }
        }catch (Exception e) {
            System.out.println("Markers are null");
        }


        for (Object name : result_table.getColumns()) {
            TableColumn f = (TableColumn) name;
            System.out.println(f.getText());
        }

        ObservableList<String> cblc = FXCollections.observableArrayList();
        listMapLocations = new ArrayList<>();
        for (int i = 0; i < this.data.size(); i++) {
            try {
                String info = "";
                int infindex = 0;
                for (Object name : result_table.getColumns()) {
                    TableColumn f = (TableColumn) name;
                    info += f.getText() + ": " + this.data.get(i).get(infindex++) + "<br/>";
                }

                String[] parts = ((String) this.data.get(i).get(datalocal)).split(",");
                System.out.println("PARTS " + parts[0] + " " + parts[1]);
                Double lat = Double.parseDouble(parts[0]);
                Double lon = Double.parseDouble(parts[1]);
                listMapLocations.add(new MapLocation(((String) this.data.get(i).get(header)), lat, lon, info));
                cblc.add(listMapLocations.get(listMapLocations.size() - 1).name);
                System.out.println("LIST MAP LOCATIONS Added" + listMapLocations);
            } catch (Exception e) {
                System.out.println(" Wrong geopos data " + e.toString());
                return;
            }
        }

        locationChoser.setItems(cblc);
        locationChoser.setVisible(true);
        try {
            List<Marker> markers = new ArrayList<>();
            for(MapLocation mpl: listMapLocations){
                markers.add(mpl.marker);
            }
            this.map.addMarkers(markers);
//
//            System.out.println("IN TRY");
//            for (MapLocation mpl : listMapLocations) {
//                System.out.println("MpL" + mpl);
//                this.map.addMarker(mpl.marker);
//            }
        }catch (Exception e){
            System.out.println("MAP LOCATION ADD MARKER EXCEPTION " + e);
        }

        System.out.println("RELOAD MAP");
        this.mapViewSettings.setVisible(true);
        this.mapView.setVisible(true);

    }

    ObservableList<PieChart.Data> Pie_chart_cdata = FXCollections.observableArrayList();

    @FXML
    public void reload_pie_chart() {
        Pie_chart_cdata.clear();

        int header = Pie_chart_header.getSelectionModel().getSelectedIndex();
        int datalocal = Pie_chart_data.getSelectionModel().getSelectedIndex();

        try {
            double f = Double.parseDouble((String) this.data.get(0).get(datalocal));
        } catch (Exception e) {
            System.out.println("reload_pie_chart 1 " + e.toString());
            return;
        }

        System.out.println(header + " " + datalocal);

        List<String> listNames = new ArrayList<>();
        //GET LINES
        String line = "";
        for (int i = 0; i < this.data.size(); i++) {
            String f = (String) this.data.get(i).get(header);
            //double d = Double.parseDouble((String) this.data.get(i).get(datalocal));
            if (!f.equals(line) && !isFound(f, listNames)) {
                line = f;
                listNames.add(line);

            }
        }

        for (String names : listNames) {
            System.out.println(names);
        }

        int[] nameData = new int[listNames.size()];
        int index = 0;
        for (String name : listNames) {
            System.out.print(name);
            for (int i = 0; i < this.data.size(); i++) {
                String f = (String) this.data.get(i).get(header);
                if (f.equals(name)) {
                    try {
                        double d = Double.parseDouble((String) this.data.get(i).get(datalocal));
                        nameData[index] += d;
                    } catch (Exception e) {
                        System.out.println("reload_pie_chart 2 " + e.toString());
                        return;
                    }
                }
            }
            System.out.println(" " + nameData[index]);
            index++;
        }

        //populate data
        for (int i = 0; i < listNames.size(); i++) {
            Pie_chart_cdata.add(new PieChart.Data(listNames.get(i) + " : " + nameData[i], nameData[i]));
        }
        pie_chart.setData(Pie_chart_cdata);
    }

    @FXML
    public void reload_area_chart() {
        int groupBy = area_chart_group.getSelectionModel().getSelectedIndex();
        int header = area_chart_header.getSelectionModel().getSelectedIndex();
        int data_ = area_chart_data.getSelectionModel().getSelectedIndex();
        int legend = area_chart_legend.getSelectionModel().getSelectedIndex();
        this.area_chart.setData(getLAList(groupBy, header, data_, legend));
    }

    @FXML
    public void reload_line_chart() {
        int groupBy = line_chart_group.getSelectionModel().getSelectedIndex();
        int header = line_chart_header.getSelectionModel().getSelectedIndex();
        int data_ = line_chart_data.getSelectionModel().getSelectedIndex();
        int legend = line_chart_legend.getSelectionModel().getSelectedIndex();
        this.line_chart.setData(getLAList(groupBy, header, data_, legend));
    }

    private ObservableList<Series<String, Double>> getLAList(int groupBy, int header, int data_, int legend) {
        List<ChartDataClass> list = getList(groupBy, header, data_, legend);
        System.out.println("Got data");

        ObservableList<Series<String, Double>> answer = FXCollections.observableArrayList();

        List<String> listNames = new ArrayList<>();
        //GET LINES
        String line = "";
        for (ChartDataClass item : list) {
            if (!item.group.equals(line)) {
                line = item.group;
                listNames.add(line);
            }
        }
        System.out.println("Lines list");
        for (String name : listNames) {
            System.out.println("Line " + name);
            Series<String, Double> _Series = new Series<>();
            _Series.setName(name);
            int i = 0;
            for (ChartDataClass item : list) {
                if (item.group.equals(name)) {
                    i++;
                    try {
                        double d = Double.parseDouble((String) item.data);
                        _Series.getData().add(new XYChart.Data(i + "", d));
                    } catch (Exception e) {
                        System.out.println("getLAList " + e.toString());
                    }
                }
            }
            answer.add(_Series);
        }
        System.out.println("End of list");
        return answer;
    }

    private List<ChartDataClass> getList(int groupBy, int header, int data, int legend) {
        List<ChartDataClass> chartData = new ArrayList<>();

        for (int i = 0; i < this.data.size(); i++) {

            String a = (String) this.data.get(i).get(groupBy);
            String b = (String) this.data.get(i).get(header);
            String c = (String) this.data.get(i).get(data);
            String d = (String) this.data.get(i).get(legend);

            chartData.add(new ChartDataClass(b, d, c, a));
        }
        Collections.sort(chartData, new CustomComparator());

        return chartData;
    }

    public class CustomComparator implements Comparator<ChartDataClass> {

        @Override
        public int compare(ChartDataClass o1, ChartDataClass o2) {
            return o1.group.compareTo(o2.group);
        }
    }

    class ChartDataClass {

        public String header, legend, group;
        public String data;
        public int comparator;

        public ChartDataClass(String header, String legend, String data, String group) {
            this.header = header;
            this.legend = legend;
            this.data = data;
            this.group = group;
        }

        @Override
        public String toString() {
            return "ChartDataClass{" + "header=" + header + ", legend=" + legend + ", group=" + group + ", data=" + data + ", comparator=" + comparator + '}';
        }
    }

    public void pdfExport() throws IOException {
        FileChooser fileChooser = new FileChooser();

        //Set extension filter
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("PDF files (*.pdf)", "*.pdf");
        fileChooser.getExtensionFilters().add(extFilter);

        //Show save file dialog
        File file = fileChooser.showSaveDialog((Stage) this.area_chart_group.getScene().getWindow());
        System.out.println("file is " + file.getPath());
        try {
            
            WritableImage image = pie_chart.snapshot(new SnapshotParameters(), null);
            if(chartPicker.getSelectionModel().getSelectedIndex() == 0){
                image = pie_chart.snapshot(new SnapshotParameters(), null);
            }else if(chartPicker.getSelectionModel().getSelectedIndex() == 1){
                image = line_chart.snapshot(new SnapshotParameters(), null);
            }else if(chartPicker.getSelectionModel().getSelectedIndex() == 2){
                image = area_chart.snapshot(new SnapshotParameters(), null);
            }else if(chartPicker.getSelectionModel().getSelectedIndex() == 3){
                image = mapView.snapshot(new SnapshotParameters(), null);
            }

            // TODO: probably use a file chooser here
            File chart = new File("chart.png");

            try {
                ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", chart);
                System.out.println("Image saved");
            } catch (IOException e) {
                System.out.println("Image save error " + e);
            }

            createPDF(file.getPath());
        } catch (DocumentException ex) {
            Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void createPDF(String pdfFilename) throws DocumentException, FileNotFoundException, IOException {

        float cellOp[] = new float[result_table.getColumns().size()];
        for (int i = 0; i < cellOp.length; i++) {
            cellOp[i] = 1;
        }

        Document document = new Document();
        
        //ADD IMAGE
        

        BaseFont bf = BaseFont.createFont("./OS.ttf", "UTF8", BaseFont.EMBEDDED);
        com.itextpdf.text.Font font = new com.itextpdf.text.Font(bf, 12, Font.NORMAL);

        PdfPTable table = new PdfPTable(cellOp);
        table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_LEFT);

        for (Object name : result_table.getColumns()) {
            TableColumn f = (TableColumn) name;
            table.addCell(f.getText());
        }
        table.setHeaderRows(1);
        PdfPCell[] cells = table.getRow(0).getCells();
        for (int j = 0; j < cells.length; j++) {
            cells[j].setBackgroundColor(BaseColor.GRAY);
        }

        for (int i = 0; i < this.data.size(); i++) {

            int infindex = 0;
            for (int j = 0; j < result_table.getColumns().size(); j++) {

                table.addCell(new Phrase(((String) data.get(i).get(infindex++)), font));
            }
        }

        PdfWriter.getInstance(document, new FileOutputStream(pdfFilename));
        document.open();
        try{
            Image image1 = Image.getInstance("chart.png");
            
            image1.scaleToFit(500, 500);
            document.add(image1);
            System.out.println("Image added");
        }catch(IOException | DocumentException io){
            System.out.println("Image add error " + io.toString());
        }
        document.add(table);
        document.close();
        System.out.println("Done");

    }

    public void xmlExport() {
        FileChooser fileChooser = new FileChooser();

        //Set extension filter
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Excel files (*.xls)", "*.xls");
        fileChooser.getExtensionFilters().add(extFilter);

        //Show save file dialog
        File file = fileChooser.showSaveDialog((Stage) this.area_chart_group.getScene().getWindow());
        System.out.println("file is " + file.getPath());

        XMLCreator(file.getPath());

    }

    public void XMLCreator(String filename) {
        try {

            HSSFWorkbook workbook = new HSSFWorkbook();
            HSSFSheet sheet = workbook.createSheet("FirstSheet");
            /*try{
                InputStream is = new FileInputStream("image1.jpeg");
                byte[] bytes = is.
                int pictureIdx = workbook.addPicture(bytes, Workbook.PICTURE_TYPE_JPEG);
                is.close();
            }catch(Exception io){
                
            }*/
            
            HSSFRow rowhead = sheet.createRow((short) 0);

            int index = 0;
            for (Object name : result_table.getColumns()) {
                TableColumn f = (TableColumn) name;
                rowhead.createCell(index++).setCellValue(f.getText());
            }

            for (int i = 0; i < this.data.size(); i++) {
                HSSFRow row = sheet.createRow((short) i + 1);
                int infindex = 0;
                for (int j = 0; j < result_table.getColumns().size(); j++) {
                    row.createCell(infindex).setCellValue(((String) data.get(i).get(infindex++)));
                }
            }
            FileOutputStream fileOut = new FileOutputStream(filename);
            workbook.write(fileOut);
            fileOut.close();
            System.out.println("Your excel file has been generated!");

        } catch (Exception ex) {
            System.out.println(ex);
        }
    }
}
