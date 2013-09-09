package tictactoe;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.GridPane;

public class GameController {
	
	private final ExecutorService executorService ;
	private final Game game ;
	private final Player humanPlayer ;
	private final ComputerPlayer computerPlayer ;
	private final MoveChoosingStrategy hintStrategy ;
	private final Map<Location, Square> squares ;
	
	public GameController(Game game, Player humanPlayer, ComputerPlayer computerPlayer) {
		this.game = game ;
		this.humanPlayer = humanPlayer ;
		this.computerPlayer = computerPlayer ;
		executorService = Executors.newSingleThreadExecutor(new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				Thread thread = new Thread(r);
				thread.setDaemon(true);
				return thread;
			}
		});
		this.hintStrategy = new UnbeatableStrategy();
		this.squares = new HashMap<>();
	}

	@FXML
	private GridPane board ;
	@FXML
	private Label statusLabel ;
	@FXML
	private Label currentPlayerLabel ;
	@FXML
	private MenuItem hintMenuItem ;

	
	public void initialize() throws IOException {
		setUpSquares();
		getComputerToMoveWhenComputerIsCurrentPlayer();
		setUpStatusLabelBindings();
		setUpHintBinding();
	}
	
	private void setUpHintBinding() {
		game.hintProperty().addListener(new ChangeListener<Location>() {
			@Override
			public void changed(ObservableValue<? extends Location> observable,
					Location oldHint, Location newHint) {
				for (Location loc : squares.keySet()) {
					final Square square = squares.get(loc);
					final ObservableList<String> styleClass = square.getStyleClass();
					if (loc.equals(newHint)) {
						if (! styleClass.contains("hint")) {
							styleClass.add("hint");
						}
					} else {
						while (styleClass.contains("hint")) {
							styleClass.remove("hint");
						}
					}
				}
			}
		});
		hintMenuItem.disableProperty().bind(game.currentPlayerProperty().isNotEqualTo(humanPlayer));
	}

	private void setUpStatusLabelBindings() {
		statusLabel.textProperty().bind(Bindings.format("%s", game.gameStatusProperty()));
		currentPlayerLabel.textProperty().bind(
			Bindings.when(
				game.currentPlayerProperty().isNotNull()
			).then(
				Bindings.format("To play: %s", game.currentPlayerProperty())
			).otherwise(
				""
			)
		);
	}

	private void getComputerToMoveWhenComputerIsCurrentPlayer() {
		game.currentPlayerProperty().addListener(new ChangeListener<Object>(){
			@Override
			public void changed(ObservableValue<? extends Object> observable,
					Object oldValue, Object newValue) {
				if (game.getCurrentPlayer() == computerPlayer) {

					// Not really necessary to choose the move in a application-threaded task, but this is to 
					// demonstrate how to implement this for a game where choosing a move may take a long time.
					// In reality here you could just do
					// game.makeMove(computerPlayer, computerPlayer.chooseMove(game));
					
					final Task<Location> computerMoveTask = new Task<Location>() {
						@Override
						public Location call() throws Exception {
							return computerPlayer.chooseMove(game);
						}
					};
					computerMoveTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
						@Override
						public void handle(WorkerStateEvent event) {
							game.makeMove(computerPlayer, computerMoveTask.getValue());
						}
					});
					executorService.submit(computerMoveTask);
				}
			}
		});
	}

	private void setUpSquares() throws IOException {
		// Arguably shouldn't really do this in the controller, but avoiding it gets messy.
		for (int column = 0 ; column < 3; column++) {
			for (int row = 0 ; row < 3 ; row++) {
				final Square square = new Square(column, row, humanPlayer, game);
				board.getChildren().add(square);
				squares.put(new Location(column, row), square);
			}
		}
	}
	
	// Event handling:
	
	@FXML
	private void newGameComputerFirst() {
		game.reset(computerPlayer);
	}
	
	@FXML 
	private void newGameHumanFirst() {
		game.reset(humanPlayer);
	}
	
	@FXML
	private void hint() {
		Location moveChoice = hintStrategy.chooseMove(game);
		game.setHint(moveChoice);
	}
	
	@FXML
	private void quit() {
		board.getScene().getWindow().hide();
	}

}