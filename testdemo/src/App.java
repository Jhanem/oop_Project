import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.text.Font;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.concurrent.Task;

import javafx.scene.input.KeyCode;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        Label label = new Label("App Reviewer");
        label.setFont(new Font("Arial Bold", 30));
        label.setStyle("-fx-text-fill: black;");
        label.setLayoutX(80);
        label.setLayoutY(50);

        TextField input = new TextField();
        input.setPromptText("Enter app name (e.g., 'Spotify')");
        input.setPrefWidth(250);
        input.setPrefHeight(35);
        input.setLayoutX(100);
        input.setLayoutY(150);

        Button submit = new Button("Submit & Review");
        submit.setLayoutX(100);
        submit.setLayoutY(190);

        // Enable submission when the Enter key is pressed in the text field
        input.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                submit.fire();
                event.consume();
            }
        });

        Pane inputPane = new Pane(label, input, submit);
        Scene scene1 = new Scene(inputPane, 500, 300);

        Label output = new Label("Search results will appear here.");
        output.setFont(new Font("Arial", 14));
        output.setWrapText(true);
        output.setLayoutX(20);
        output.setLayoutY(20);
        output.setPrefWidth(460);

        Button back = new Button("Back");
        back.setLayoutX(20);
        back.setLayoutY(250);

        Pane outputPane = new Pane(output, back);
        Scene scene2 = new Scene(outputPane, 500, 300);

        submit.setOnAction(e -> {
            final String appName = input.getText().trim();

            if (appName.isEmpty()) {
                output.setText("Please enter an app name.");
                stage.setScene(scene2);
                return;
            }

            output.setText("Searching Google Play for: " + appName + "...\nPlease wait.");
            stage.setScene(scene2);

            Task<String> apiTask = new Task<>() {
                @Override
                protected String call() throws Exception {
                    return ApifyScraperService.callApifyApi(appName);
                }
            };

            apiTask.setOnSucceeded(event -> {
                String result = apiTask.getValue();
                output.setText(result);
            });

            apiTask.setOnFailed(event -> {
                output.setText("❌ Error: Could not fetch data. Check API token, credits, or network.");
                event.getSource().getException().printStackTrace();
            });

            new Thread(apiTask).start();
        });

        back.setOnAction(e -> {
            input.clear();
            stage.setScene(scene1);
        });


        stage.setTitle("App Reviewer Tool");
        stage.setScene(scene1);
        stage.setResizable(false);
        stage.show();  
    }

    public static void main(String[] args) {
        launch();
    }
}