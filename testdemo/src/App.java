import javafx.application.Application;
import javafx.fxml.FXML; 
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox; 
import javafx.scene.layout.StackPane;
import javafx.scene.input.KeyEvent;
import javafx.event.ActionEvent;
import javafx.stage.Stage;
import javafx.concurrent.Task;
import java.io.IOException;

public class App extends Application {

    @FXML private StackPane mainStack;
    @FXML private VBox inputPane;
    @FXML private VBox outputPane;
    @FXML private TextField input; 
    
    @FXML private Label output; 

    @FXML private Label appNameLabel;
    @FXML private Label ratingLabel;
    @FXML private Label summaryLabel;
    @FXML private Label userReviewLabel;


    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("AppReviewer.fxml")); 
        
        try {
            Scene scene = new Scene(fxmlLoader.load(), 800, 600);
            stage.setTitle("App Reviewer Tool");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();
        } catch (IOException e) {
            System.err.println("Failed to load FXML file: AppReviewer.fxml. Ensure all fx:ids are correct.");
            e.printStackTrace();
            throw e;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
    
    @FXML
    void handleSubmitAction(ActionEvent event) {
        if (input == null) return; 
        
        final String appName = input.getText().trim();

        if (appName.isEmpty()) {
            if (summaryLabel != null) summaryLabel.setText("Please enter an app name."); 
            if (inputPane != null) inputPane.setVisible(false);
            if (outputPane != null) outputPane.setVisible(true);
            return;
        }

        if (appNameLabel != null) appNameLabel.setText("App: " + appName);
        if (ratingLabel != null) ratingLabel.setText("Rating: Searching...");
        if (userReviewLabel != null) userReviewLabel.setText("Review: Searching..."); 
 
        if (inputPane != null) inputPane.setVisible(false);
        if (outputPane != null) outputPane.setVisible(true);

        Task<String> apiTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                return ApifyScraperService.callApifyApi(appName);
            }
        };

        apiTask.setOnSucceeded(succeededEvent -> {
            String resultString = apiTask.getValue();
            
            if (resultString.startsWith("❌ Error") || resultString.startsWith("⚠️")) {
                if (appNameLabel != null) appNameLabel.setText("App: " + appName);
                if (ratingLabel != null) ratingLabel.setText("Rating: N/A");
                if (userReviewLabel != null) userReviewLabel.setText("Review: N/A"); 
                if (summaryLabel != null) summaryLabel.setText(resultString);
                return;
            }
            
            String title = "N/A", rating = "N/A", summary = "N/A", userReview = "No review found.";
            
            String[] lines = resultString.split("\\r?\\n");
            
            if (lines.length > 0 && lines[0].startsWith("✅ App Data Fetched Successfully for: ")) {
                title = lines[0].substring("✅ App Data Fetched Successfully for: ".length()).trim();
            }
            
            for (String line : lines) {
                if (line.contains("Rating:")) {
                    rating = line.substring(line.indexOf("Rating:") + "Rating:".length()).trim();
                    break;
                }
            }

            int reviewStartIndex = resultString.indexOf("User Review:");
            if (reviewStartIndex != -1) {
                String reviewSection = resultString.substring(reviewStartIndex + " User Review:".length()).trim();
                
                int nextFieldIndex = reviewSection.indexOf("Summary:");
                if (nextFieldIndex != -1) {
                    userReview = reviewSection.substring(0, nextFieldIndex).trim();
                } else {
                    userReview = reviewSection;
                }
            }

            int summaryIndex = resultString.indexOf("Summary:");
            if (summaryIndex != -1) {
                 String summaryLabelText = " Summary:";
                 if (resultString.substring(summaryIndex + summaryLabelText.length()).trim().startsWith("\n")) {
                      summaryLabelText += "\n";
                 }
                 summary = resultString.substring(summaryIndex + summaryLabelText.length()).trim();
            }

            if (appNameLabel != null) appNameLabel.setText("App: " + title);
            if (ratingLabel != null) ratingLabel.setText("Rating: " + rating);
            if (userReviewLabel != null) userReviewLabel.setText("Review: " + userReview);
            if (summaryLabel != null) summaryLabel.setText(summary);
        });

        apiTask.setOnFailed(failedEvent -> {
            if (summaryLabel != null) summaryLabel.setText("❌ Fatal Error: " + failedEvent.getSource().getException().getMessage());
            if (appNameLabel != null) appNameLabel.setText("App: Failed");
            if (ratingLabel != null) ratingLabel.setText("Rating: Failed");
            if (userReviewLabel != null) userReviewLabel.setText("Review: Failed"); 
            failedEvent.getSource().getException().printStackTrace();
        });

        new Thread(apiTask).start();
    }

    @FXML
    void handleBackAction(ActionEvent event) {
        if (input != null) input.clear();
        
        if (outputPane != null) outputPane.setVisible(false);
        if (inputPane != null) inputPane.setVisible(true);
        
        if (appNameLabel != null) appNameLabel.setText("App: (Loading...)");
        if (ratingLabel != null) ratingLabel.setText("Rating: (Loading...)");
        if (userReviewLabel != null) userReviewLabel.setText("Review: (Loading...)");
        if (summaryLabel != null) summaryLabel.setText("Summary: (Loading...)");
    }

    @FXML
    private void handleReviewLinkAction() {
        System.out.println("Review link action triggered.");
    }
    
    @FXML
    void handleEnterKey(KeyEvent event) {
        if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
            handleSubmitAction(null); 
            event.consume(); 
        }
    }
}