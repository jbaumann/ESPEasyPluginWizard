package de.xinaris.espeasypluginwizard;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javafx.application.Application;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.transformation.SortedList;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

/**
 * ESPEasy Firmware Plugin Configurator.
 */
public class App extends Application {

	private static final Color UNMODIFIED_TEXT_COLOR = Color.BLACK;
	private static final Color MODIFIED_TEXT_COLOR = Color.RED;

	private static ConfigurationData config;
	private static Model model;

	private Label currentMemLabel = null;
	private final List<Label> memLabels = new ArrayList<>();

	private Stage stage; // needed for dialogs
	private TableView<PluginData> tv = new TableView<>(); // needed after save
	private int modifiedCount = 1; // we start with an unsaved config
	private Label srcDirectory;

	/**
	 * The main method first tries to read the config file, then the plugin file
	 * and then starts the user interface.
	 *
	 * @param args
	 *            command line arguments
	 */
	public static void main(final String[] args) {
		config = new ConfigurationData(args);

		try {
			final String pluginData =
					config.getConfig(ConfigurationData.PLUGIN_DATA).toString();
			final String prefixPattern =
					config.getConfig(ConfigurationData.PLUGIN_PREFIX_PATTERN)
							.toString();
			final String suffix = config
					.getConfig(ConfigurationData.PLUGIN_SUFFIX).toString();
			model = new Model(pluginData, prefixPattern, suffix);

			// Important, the plugin data has to be read first
			final String dirName = config
					.getConfig(ConfigurationData.SRC_DIRECTORY).toString();
			File df = new File(dirName);
			if (df.exists()) {
				model.setSrcDir(df.getCanonicalPath());
			} else {
				df = new File(".");
				model.setSrcDir(df.getCanonicalPath());
			}

			model.setFileName(
					config.getConfig(ConfigurationData.PLUGIN_HEADER_FILE)
							.toString());
			@SuppressWarnings("unchecked")
			final List<Map<String, Object>> limits =
					(List<Map<String, Object>>) config
							.getConfig(ConfigurationData.MEM_LIMITS);
			model.initMemLimits(limits);

		} catch (final IOException e) {
			System.exit(0);
		}

		launch(args);
	}

	/**
	 * This method defines the graphical interface for the Wizard.
	 */

	@Override
	public void start(final Stage primaryStage) throws Exception {
		stage = primaryStage;
		createUI();
		stage.show();
	}

	/**
	 * This method creates the UI for the wizard.
	 */
	private void createUI() {
		final int gap = 10;
		// final int paneWidth = 7 * 100; // number of columns
		final GridPane root = new GridPane();
		root.setAlignment(Pos.CENTER);
		root.setHgap(gap);
		root.setVgap(gap);
		root.setPadding(new Insets(gap));
		// root.setPrefWidth(paneWidth);

		// Restraints for the three columns, the middle one grows
		ColumnConstraints cc = new ColumnConstraints();
		root.getColumnConstraints().add(cc);
		cc = new ColumnConstraints();
		cc.setHgrow(Priority.ALWAYS);
		root.getColumnConstraints().add(cc);
		cc = new ColumnConstraints();
		root.getColumnConstraints().add(cc);

		// we have three columns and increment the rows for each major UI
		// element
		final int numColumns = 3;
		int row = 0;
		HBox hb;

		// The Source Directory
		final Label description = new Label("Source-Dir:");

		srcDirectory = new Label(model.getSrcDir());
		srcDirectory.setStyle("-fx-font-weight: bold;");
		root.add(description, 0, row);

		srcDirectory.setTextAlignment(TextAlignment.LEFT);
		srcDirectory.setMaxWidth(Double.MAX_VALUE);
		srcDirectory.setOnMouseClicked(e -> changeDirButtonPressed(e));

		hb = new HBox(srcDirectory);
		HBox.setHgrow(hb, Priority.ALWAYS);
		hb.setAlignment(Pos.CENTER);
		root.add(hb, 1, row);

		final Button changeDir = new Button("...");
		changeDir.setOnAction(e -> changeDirButtonPressed(e));
		hb = new HBox(changeDir);
		HBox.setHgrow(hb, Priority.ALWAYS);
		hb.setAlignment(Pos.CENTER_RIGHT);

		root.add(hb, 2, row);
		row++;

		// The table view
		tv = createTableView();
		root.add(tv, 0, row++, numColumns, 1);

		// The list of memory limit labels
		final MemoryData currentMem = getCurrentMemData();
		root.add(new Label(currentMem.getName()), 0, row);
		currentMemLabel = new Label(currentMem.formatContents());
		root.add(currentMemLabel, 1, row++);

		for (final MemoryData md : model.getMemLimits()) {
			final Label memLimitLabel = new Label(md.formatContents());
			memLimitLabel.setTextFill(UNMODIFIED_TEXT_COLOR);
			memLabels.add(memLimitLabel);
			root.add(new Label(md.getName()), 0, row);
			root.add(memLimitLabel, 1, row);
			row++;
		}
		recalcValues(null);
		// the load file button
		final Button loadFile = new Button("Load");
		loadFile.setOnAction(e -> loadButtonPressed(e));
		hb = new HBox(loadFile);
		HBox.setHgrow(hb, Priority.ALWAYS);
		hb.setAlignment(Pos.CENTER_LEFT);
		root.add(hb, 0, row);

		// the save file button
		final Button saveFile = new Button("Save");
		saveFile.setOnAction(e -> saveButtonPressed(e));
		hb = new HBox(saveFile);
		HBox.setHgrow(hb, Priority.ALWAYS);
		hb.setAlignment(Pos.CENTER);
		root.add(hb, numColumns - 1, row);
		stage.setScene(new Scene(root));
		stage.setTitle(generateTitle());
		// Turn off the arbitrary selection of button
		root.requestFocus();
		// root.setGridLinesVisible(true);
	}

	/**
	 * Open a Directory Chooser and accept a new directory containing the source
	 * files.
	 *
	 * @param e
	 *            the event associated to the button press
	 * @return null
	 */
	private Object changeDirButtonPressed(final Event e) {
		final DirectoryChooser dc = new DirectoryChooser();
		dc.setTitle("Choose the Source Directory");
		dc.setInitialDirectory(new File(model.getSrcDir()));
		final File dir = dc.showDialog(stage);
		if (dir != null) {
			final String newDir = dir.getAbsolutePath();
			model.setSrcDir(newDir);
			srcDirectory.setText(newDir);
			tv.refresh();
			return dir;
		}
		return null;
	}

	/**
	 * Generate the Title for the Window.
	 *
	 * @return the current title string
	 */
	private String generateTitle() {
		@SuppressWarnings("checkstyle:avoidinlineconditionals")
		final String modifiedString = modifiedCount == 0 ? "   " : " * ";
		return "ESPEasy Plugin Wizard - " + model.getFileName()
				+ modifiedString;
	}

	/**
	 * Calculate current memory usage.
	 *
	 * @return MemoryData object containing the current memory usage
	 */
	private MemoryData getCurrentMemData() {
		int cacheIRam = 0, initRam = 0, roRam = 0, uninitRam = 0, flashRom = 0;

		for (final PluginData p : model.getPluginData()) {
			if (p.isEnabled()) {
				cacheIRam += p.getCacheIRam();
				initRam += p.getInitRam();
				roRam = p.getRoRam();
				uninitRam += p.getUninitRam();
				flashRom += p.getFlashRom();
			}
		}

		return new MemoryData("Current", cacheIRam, initRam, roRam, uninitRam,
				flashRom);
	}

	/**
	 * Create the table view containing the plugin data details.
	 *
	 * @return TableView object
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private TableView<PluginData> createTableView() {
		// plugin |cache IRAM |init RAM |r.o. RAM |uninit RAM |Flash ROM
		tv.setEditable(true);

		final TableColumn<PluginData, String> nameCol =
				new TableColumn<>("Name");
		nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
		final TableColumn<PluginData, String> cacheIRamCol =
				new TableColumn<>("Cache IRAM");
		cacheIRamCol
				.setCellValueFactory(new PropertyValueFactory<>("cacheIRam"));
		final TableColumn<PluginData, String> initRamCol =
				new TableColumn<>("Init. RAM");
		initRamCol.setCellValueFactory(new PropertyValueFactory<>("initRam"));
		final TableColumn<PluginData, String> roRamCol =
				new TableColumn<>("R/O RAM");
		roRamCol.setCellValueFactory(new PropertyValueFactory<>("roRam"));
		final TableColumn<PluginData, String> uninitRamCol =
				new TableColumn<>("Uninit. RAM");
		uninitRamCol
				.setCellValueFactory(new PropertyValueFactory<>("uninitRam"));
		final TableColumn<PluginData, String> flashRomCol =
				new TableColumn<>("Flash ROM");
		flashRomCol.setCellValueFactory(new PropertyValueFactory<>("flashRom"));
		// the checkbox
		final TableColumn<PluginData, String> enabledCol = new TableColumn<>();
		enabledCol.setCellValueFactory(wrapper -> {
			final PluginData plugin = wrapper.getValue();
			final CheckBox checkBox = new CheckBox();
			checkBox.selectedProperty().setValue(plugin.isEnabled());
			checkBox.selectedProperty().addListener(
					(ChangeListener<Boolean>) (observable, oldVal, newVal) -> {
						plugin.setEnabled(newVal);
						if (plugin.isModified()) {
							modifiedCount++;
						} else {
							modifiedCount--;
						}
						recalcValues(checkBox);
						tv.refresh();
					});
			if (plugin.isReadOnly()) {
				checkBox.setDisable(true);
			}
			return new SimpleObjectProperty(checkBox);
		});

		// Coloring the table rows when modified
		tv.setRowFactory(tableRow -> new TableRow<PluginData>() {
			@Override
			public void updateItem(final PluginData plugin,
					final boolean empty) {
				super.updateItem(plugin, empty);
				if (plugin == null) {
					setStyle("");
				} else if (plugin.isModified()) {
					setStyle(config.getConfig(ConfigurationData.MODIFIED_STYLE)
							.toString());
				} else {
					setStyle(
							config.getConfig(ConfigurationData.UNMODIFIED_STYLE)
									.toString());
				}
			}
		});
		// set name column to grow as much as possible
		final double width = enabledCol.widthProperty().get()
				+ cacheIRamCol.widthProperty().get()
				+ initRamCol.widthProperty().get()
				+ roRamCol.widthProperty().get()
				+ uninitRamCol.widthProperty().get()
				+ flashRomCol.widthProperty().get() + 10;

		nameCol.prefWidthProperty().bind(tv.widthProperty().subtract(width));

		// plugin |cache IRAM |init RAM |r.o. RAM |uninit RAM |Flash ROM
		tv.getColumns().addAll(enabledCol, nameCol, cacheIRamCol, initRamCol,
				roRamCol, uninitRamCol, flashRomCol);

		tv.minWidthProperty().bind(tv.prefWidthProperty());

		final SortedList<PluginData> sl =
				new SortedList<>(model.getPluginData());
		// tv.setPrefWidth(600);
		tv.setItems(sl);
		sl.comparatorProperty().bind(tv.comparatorProperty());
		return tv;
	}

	/**
	 * This method is called when the Save Button is pressed.
	 *
	 * @param e
	 *            The event
	 */
	private void saveButtonPressed(final Event e) {

		final FileChooser fc = new FileChooser();
		fc.setTitle("Save Plugin Configuration");
		fc.setInitialDirectory(new File(model.getSrcDir()));
		fc.setInitialFileName(model.getFileName());
		fc.setSelectedExtensionFilter(new ExtensionFilter("Headerfile", "*.h"));
		final File saveFile = fc.showSaveDialog(stage);
		// saveFile is null when Cancel is pressed
		if (saveFile != null && !saveFile.getName().equals("")) {
			model.setFileName(saveFile.getName());
			final boolean success = model.save(saveFile);
			if (success) {
				modifiedCount = 0;
				for (final PluginData pl : model.getPluginData()) {
					pl.setModified(false);
				}
				stage.setTitle(generateTitle());
				tv.refresh();
			}
		}
	}

	/**
	 * This method is called when the Load Button is pressed.
	 *
	 * @param e
	 *            The event
	 */
	private void loadButtonPressed(final Event e) {

		final FileChooser fc = new FileChooser();
		fc.setTitle("Load Plugin Configuration");
		fc.setInitialDirectory(new File(model.getSrcDir()));
		fc.setInitialFileName(model.getFileName());
		fc.setSelectedExtensionFilter(new ExtensionFilter("Headerfile", "*.h"));
		final File loadFile = fc.showOpenDialog(stage);
		// saveFile is null when Cancel is pressed
		if (loadFile != null && !loadFile.getName().equals("")) {
			model.setFileName(loadFile.getName());
			final boolean success = model.load(loadFile,
					config.getConfig(ConfigurationData.PLUGIN_PREFIX_PATTERN)
							.toString());
			if (success) {
				modifiedCount = 0;
				stage.setTitle(generateTitle());
				tv.refresh();
			}
		}
	}

	/**
	 * This method recalculates the memory requirements for the enabled plugins.
	 *
	 * @param source
	 *            the checkbox that has been clicked
	 */
	private void recalcValues(final CheckBox source) {
		stage.setTitle(generateTitle());

		// Memory Limit Labels
		final MemoryData currentMem = getCurrentMemData();
		currentMemLabel.setText(currentMem.formatContents());
		for (int i = 0; i < model.getMemLimits().size(); i++) {
			if (model.exceedsMemReference(currentMem,
					model.getMemLimits().get(i))) {
				memLabels.get(i).setTextFill(MODIFIED_TEXT_COLOR);
			} else {
				memLabels.get(i).setTextFill(UNMODIFIED_TEXT_COLOR);
			}

		}
	}

}
