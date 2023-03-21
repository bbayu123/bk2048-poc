/* **********************************************************************
 * Copyright (C) 2023 Cyrus Mian Xi Li (bbayu/bbayu123)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * **********************************************************************
 */
package io.github.bbayu123.bk2048;

import java.awt.Dimension;
import java.awt.Point;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.stream.IntStream;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import com.bergerkiller.bukkit.common.events.map.MapClickEvent;
import com.bergerkiller.bukkit.common.events.map.MapKeyEvent;
import com.bergerkiller.bukkit.common.events.map.MapStatusEvent;
import com.bergerkiller.bukkit.common.map.MapCanvas;
import com.bergerkiller.bukkit.common.map.MapColorPalette;
import com.bergerkiller.bukkit.common.map.MapDisplay;
import com.bergerkiller.bukkit.common.map.MapDisplayProperties;
import com.bergerkiller.bukkit.common.map.MapEventPropagation;
import com.bergerkiller.bukkit.common.map.MapFont;
import com.bergerkiller.bukkit.common.map.MapPlayerInput;
import com.bergerkiller.bukkit.common.map.MapPlayerInput.Key;
import com.bergerkiller.bukkit.common.map.MapSessionMode;
import com.bergerkiller.bukkit.common.map.MapTexture;
import com.bergerkiller.bukkit.common.map.widgets.MapWidget;
import com.bergerkiller.bukkit.common.map.widgets.MapWidgetButton;
import com.bergerkiller.bukkit.common.map.widgets.MapWidgetText;
import com.bergerkiller.bukkit.common.map.widgets.MapWidgetWindow;
import com.bergerkiller.bukkit.common.nbt.CommonTagCompound;
import com.bergerkiller.bukkit.common.utils.ItemUtil;

/**
 * This is the main plugin class
 * <p>
 * Contains boiler-plate code for the plugin to work.
 *
 * @author Cyrus Mian Xi Li (bbayu/bbayu123)
 *
 */
public class Main extends JavaPlugin {

	/**
	 * {@inheritDoc}
	 * <p>
	 * What we are doing here is linking the command executor to our plugin.
	 */
	@Override
	public void onEnable() {
		this.getCommand("2048").setExecutor(this);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Since only players can hold items, we check if it is a player before
	 * continuing.
	 * <p>
	 * When the player does {@code /2048 get}, then we create the map item using
	 * {@link MapDisplay#createMapItem(Class)}, and give this to the player.
	 * <p>
	 * If we need to pass in parameters/properties to the display, we use
	 * {@link ItemUtil#getMetaTag(ItemStack)} and then call
	 * {@link CommonTagCompound#putValue(String, Object) putValue(String, Object)}
	 * on the tag to add properties.
	 */
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String cmdLabel, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage("You must be a player in order to do this!");
			return true;
		}

		Player player = (Player) sender;

		if (args.length == 0) {
			return false;
		}
		if (args[0].equalsIgnoreCase("get")) {
			ItemStack item = MapDisplay.createMapItem(TwoZeroFourEight.class);
			ItemUtil.getMetaTag(item).putValue("owner", player.getUniqueId());
			ItemUtil.setDisplayName(item, "2048");
			player.getInventory().addItem(item);
			player.sendMessage(ChatColor.GREEN + "Obtained 2048");
		}
		return true;
	}

	/**
	 * A simple check to see if the clicked position is within the bounds of the
	 * target widget
	 *
	 * @param widget   the target widget that we want to check bounds for
	 * @param clickedX the X-position of the click
	 * @param clickedY the Y-position of the click
	 * @return whether the click was within the bounds of the widget, or
	 *         {@code false} if the widget is {@code null}
	 *
	 * @see MapWidget#getAbsoluteX()
	 * @see MapWidget#getAbsoluteY()
	 * @see MapWidget#getWidth()
	 * @see MapWidget#getHeight()
	 */
	public static boolean isInBounds(MapWidget widget, int clickedX, int clickedY) {
		if (widget == null) {
			return false;
		}

		return clickedX >= widget.getAbsoluteX() && clickedX <= widget.getAbsoluteX() + widget.getWidth()
				&& clickedY >= widget.getAbsoluteY() && clickedY <= widget.getAbsoluteY() + widget.getHeight();
	}

	/**
	 * Draws a filled rounded rectangle on the given canvas with a given color
	 *
	 * @param view  the canvas to draw on
	 * @param color the color of the rounded rectangle
	 */
	public static void fillRoundedRectangle(MapCanvas view, byte color) {
		int w = view.getWidth(), h = view.getHeight();
		view.drawContour(
				Arrays.asList(new Point(0, 3), new Point(3, 0), new Point(w - 4, 0), new Point(w - 1, 3),
						new Point(w - 1, h - 4), new Point(w - 4, h - 1), new Point(3, h - 1), new Point(0, h - 4)),
				color);

		view.drawLine(3, 1, w - 4, 1, color);
		view.drawLine(3, h - 2, w - 4, h - 2, color);
		view.drawLine(1, 3, 1, h - 4, color);
		view.drawLine(w - 2, 3, w - 2, h - 4, color);

		view.fillRectangle(2, 2, w - 4, h - 4, color);
	}

	/**
	 * This is the main driver class for the 2048 game
	 * <p>
	 * This is a {@link MapDisplay} class. Use this class to understand how to use
	 * {@code MapDisplay}s.
	 * <p>
	 * It is important to note that this class must have {@code public} visibility.
	 * Having {@code protected}, package (default), or {@code private} visibility
	 * will not work.
	 *
	 * @author Cyrus Mian Xi Li (bbayu/bbayu123)
	 *
	 */
	public static class TwoZeroFourEight extends MapDisplay {
		/**
		 * Whether walking by sneaking is enabled or not.
		 */
		private boolean sneakWalking = false;

		/**
		 * Holds the minesweeper board.
		 */
		private TwoZeroFourEightBoard board = null;

		/**
		 * The owner of this minesweeper display. Unused in this example.
		 */
		@SuppressWarnings("unused")
		private UUID owner = null;

		/**
		 * {@inheritDoc}
		 * <p>
		 * We use this method to initialize a {@code MapDisplay}.
		 * <p>
		 * <b>We do not use the constructor to initialize a {@code MapDisplay}.</b>
		 * <p>
		 * If we have passed properties to the display, we use the {@code properties}
		 * object and call {@link MapDisplayProperties#get(String, Class) get(String,
		 * Class)} to re-call them.
		 * <p>
		 * This method only sets up the behavior of the display. We use a separate
		 * method to handle the content of the display.
		 *
		 * @see {@link MapDisplay#properties} for more information about the properties
		 *      object
		 * @see {@link MapDisplayProperties#get(String, Class)} to get properties that
		 *      were stored
		 * @see {@link #reload()} for more information on how the content is handled
		 */
		@Override
		public void onAttached() {
			this.owner = this.properties.get("owner", UUID.class);

			this.setGlobal(true);
			this.setUpdateWithoutViewers(false);
			this.setSessionMode(MapSessionMode.VIEWING);
			this.setMasterVolume(0.3f);
			this.reload();
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * We use this method to override map input to allow walking while sneaking
		 */
		@Override
		public void onTick() {
			if (this.getViewers().size() == 0) {
				return;
			}
			Player player = this.getViewers().get(0);

			// Allow walking around when sneaking
			if (this.sneakWalking && !player.isSneaking()) {
				this.sneakWalking = false;
				this.setReceiveInputWhenHolding(true);
			}
		}

		/**
		 * Reloads the contents of this display
		 * <p>
		 * Here, we do several things:
		 * <ol>
		 * <li>Clear all existing widgets
		 * <li>Initialize the 2048 board widget
		 * <li>Fill the background with a gray color
		 * <li>Add the board widget to the display
		 * <li>Update sneak walking
		 * </ol>
		 *
		 * @see {@link MapDisplay#clearWidgets()} to clear all widgets
		 * @see {@link MapDisplay#getLayer()} to get layer 0 (which is usually the
		 *      background)
		 * @see {@link Layer#fillRectangle(int, int, int, int, byte)} to fill a
		 *      rectangle
		 * @see {@link MapColorPalette#getColor(int, int, int)} to get a byte color
		 *      representation
		 * @see {@link MapDisplay#addWidget(MapWidget)} to add the widget to the display
		 */
		public void reload() {
			this.clearWidgets();

			this.board = new TwoZeroFourEightBoard();
			this.board.setState(GameState.TITLE);

			this.getLayer().fillRectangle(0, 0, this.getWidth(), this.getHeight(),
					MapColorPalette.getColor(223, 223, 223));
			this.addWidget(this.board);

			this.sneakWalking = this.getOwners().get(0).isSneaking();
			this.setReceiveInputWhenHolding(!this.sneakWalking);
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * We use this method to update sneak walking.
		 *
		 * @see #updateSneakWalking(MapKeyEvent)
		 */
		@Override
		public void onKeyPressed(MapKeyEvent event) {
			super.onKeyPressed(event);
			this.updateSneakWalking(event);
		}

		/**
		 * Updates sneak walking based on the received key event
		 *
		 * @param event the key press event that was received
		 * @return whether sneak walking should be allowed or not
		 */
		private boolean updateSneakWalking(MapKeyEvent event) {
			if (event.getKey() == MapPlayerInput.Key.BACK) {
				this.setReceiveInputWhenHolding(false);
				this.getOwners().get(0).setSneaking(true);
				this.sneakWalking = true;
				return true;
			}
			return false;
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * We use this to cancel the default behavior of the click event, and notify the
		 * clicked position to all child widgets.
		 *
		 * @param event the event containing the map click
		 * @see {@link MapDisplay#sendStatusChange(String, Object)} for notifying the
		 *      entire display
		 * @see {@link MapWidget#sendStatusChange(MapEventPropagation, String, Object)}
		 *      for notifying a specific widget with a direction to propagate the
		 *      message
		 * @see {@link #onRightClick(MapClickEvent)} for the exact same thing but
		 *      handling right-click instead
		 */
		@Override
		public void onLeftClick(MapClickEvent event) {
			if (event.getPlayer().isSneaking()) {
				return;
			}
			event.setCancelled(true);
			this.board.sendStatusChange(MapEventPropagation.UPSTREAM, "LEFT_CLICK",
					new Point(event.getX(), event.getY()));
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * We use this to cancel the default behavior of the click event, and notify the
		 * clicked position to all child widgets.
		 *
		 * @param event the event containing the map click
		 * @see {@link MapDisplay#sendStatusChange(String, Object)} for notifying the
		 *      entire display
		 * @see {@link MapWidget#sendStatusChange(MapEventPropagation, String, Object)}
		 *      for notifying a specific widget with a direction to propagate the
		 *      message
		 * @see {@link #onLeftClick(MapClickEvent)} for the exact same thing but
		 *      handling left-click instead
		 */
		@Override
		public void onRightClick(MapClickEvent event) {
			if (event.getPlayer().isSneaking()) {
				return;
			}
			event.setCancelled(true);
			this.board.sendStatusChange(MapEventPropagation.UPSTREAM, "RIGHT_CLICK",
					new Point(event.getX(), event.getY()));
		}
	}

	/**
	 * This is the board widget class for the 2048 game
	 * <p>
	 * This is a {@link MapWidget} class. Use this class to understand how to use
	 * {@code MapWidget}s.
	 * <p>
	 * This widget handles all the game logic of 2048. The appearance and visuals
	 * are handled by each child widget.
	 * <p>
	 * This class can have any visibility, as long as it is visible to the display
	 * class.
	 *
	 * @author Cyrus Mian Xi Li (bbayu/bbayu123)
	 *
	 * @see {@link TwoZeroFourEightTile} for another widget class
	 */
	private static class TwoZeroFourEightBoard extends MapWidget {
		/**
		 * The minimum border thickness
		 */
		private static final int MINIMUM_BORDER = 1;

		/**
		 * The pixel position of the top left of the first tile
		 */
		private static final int TOP_LEFT_TILE = 9;
		/**
		 * The offset between tiles
		 */
		private static final int TILE_OFFSET = 28;
		/**
		 * The tile size
		 */
		private static final int TILE_SIZE = 25;

		/**
		 * Number of rows
		 */
		private static final int BOARD_ROWS = 4;
		/**
		 * Number of columns
		 */
		private static final int BOARD_COLS = 4;

		/**
		 * The global randomizer
		 */
		private static final Random RANDOM = new Random();

		/**
		 * The value to set the counter to when movement happens.
		 */
		private static final int MOVEMENT_FRAMES = 4;

		/**
		 * The current state of the game
		 */
		private GameState state = GameState.TITLE;

		/**
		 * The list of 2048 tiles on the board
		 */
		private List<TwoZeroFourEightTile> tiles = null;

		/**
		 * The score of the game
		 */
		private int score = 0;

		/**
		 * A counter showing how many ticks remaining in the movement
		 */
		private int movementCounter = 0;
		/**
		 * A counter showing how many ticks since the last win/lose check
		 */
		private int winLoseCounter = 0;

		/**
		 * The continue mode flag, allows continuing after 2048 is reached
		 */
		private boolean continueMode = false;

		/**
		 * Creates a TwoZeroFourEightBoard
		 * <p>
		 * Here we ensure that the board is focusable, which is required to allow key
		 * presses to be received without any focusable child widgets.
		 */
		public TwoZeroFourEightBoard() {
			this.setFocusable(true);
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * Here we set the bounds of the widget, and initiate the loading of the board.
		 *
		 * @see {@link #reload()} for more information on how the board is loaded
		 */
		@Override
		public void onAttached() {
			super.onAttached();
			this.setBounds(TwoZeroFourEightBoard.MINIMUM_BORDER, TwoZeroFourEightBoard.MINIMUM_BORDER,
					this.display.getWidth() - TwoZeroFourEightBoard.MINIMUM_BORDER * 2,
					this.display.getHeight() - TwoZeroFourEightBoard.MINIMUM_BORDER * 2);
			this.reload();
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * Here we tick the movement counter and the win/lose counter, and act
		 * accordingly.
		 */
		@Override
		public void onTick() {
			if (this.tiles == null) {
				return;
			}

			if (this.movementCounter > 0) {
				if (--this.movementCounter == 0) {
					// Generate new tile
					{
						int index = this.findRandomEmptyTile();
						int value = TwoZeroFourEightBoard.getRandomStartingValue();

						TwoZeroFourEightTile tile = new TwoZeroFourEightTile(value);
						Point rowCol = this.getRowColFromIndex(index);
						tile.setBounds(this.getPixelPosFromIndex(rowCol.y), this.getPixelPosFromIndex(rowCol.x),
								TwoZeroFourEightBoard.TILE_SIZE, TwoZeroFourEightBoard.TILE_SIZE);
						this.tiles.set(index, tile);
						this.addWidget(tile);
					}
				}
			}

			if (this.state != GameState.GAME) {
				return;
			}

			if (++this.winLoseCounter > 10) {
				// Don't check if the tiles are still moving
				if (this.tiles.stream().filter(Objects::nonNull).anyMatch(TwoZeroFourEightTile::isMoving)) {
					return;
				}

				// Check if the 2048 tile has appeared
				if (!this.continueMode
						&& this.tiles.stream().filter(Objects::nonNull).anyMatch(tile -> tile.getValue() == 2048)) {
					this.setState(GameState.WIN);
				}

				// Check if it is no longer possible to move
				// Matching condition is if all tiles are filled AND if each tile doesn't have
				// adjacent of same value
				if (this.tiles.stream().allMatch(Objects::nonNull)) {
					Point[] adjacents = new Point[] { new Point(0, 1), new Point(1, 0) };

					if (
					// Get all rows
					IntStream.range(0, TwoZeroFourEightBoard.BOARD_ROWS).boxed()
							// Combine with all columns
							.flatMap(row -> IntStream.range(0, TwoZeroFourEightBoard.BOARD_COLS)
									.mapToObj(col -> new Point(row, col)))
							// Combine with adjacents to make pairs
							.flatMap(rowCol -> Arrays.stream(adjacents)
									.map(adjacent -> new Point(rowCol.x + adjacent.x, rowCol.y + adjacent.y))
									.map(newRowCol -> new Pair<>(rowCol, newRowCol)))
							// Filter invalid secondary points
							.filter(pair -> this.getTile(pair.second().x, pair.second().y) != null)
							// Map to pair of tiles
							.map(pair -> new Pair<>(this.getTile(pair.first().x, pair.first().y),
									this.getTile(pair.second().x, pair.second().y)))
							// ALL MATCH pairs have different values
							.allMatch(pair -> pair.first().getValue() != pair.second().getValue())) {
						this.setState(GameState.LOSE);
					}
				}
			}
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * In here, we draw 2 lines of text within the widget bounds using different
		 * fonts.
		 *
		 * @see {@link MapWidget#view} for obtaining the canvas that is bound by the
		 *      widget
		 * @see {@link MapCanvas#draw(MapFont, int, int, byte, CharSequence)} for
		 *      drawing the text onto the canvas
		 */
		@Override
		public void onDraw() {
			if (this.tiles != null) {
				this.view.fillRectangle(0, 0, this.getWidth(), this.getHeight(),
						MapColorPalette.getColor(158, 148, 137));

				for (int row = 0; row < TwoZeroFourEightBoard.BOARD_ROWS; row++) {
					for (int col = 0; col < TwoZeroFourEightBoard.BOARD_COLS; col++) {
						int x = this.getPixelPosFromIndex(col);
						int y = this.getPixelPosFromIndex(row);
						MapCanvas tileView = this.view.getView(x, y, TwoZeroFourEightBoard.TILE_SIZE,
								TwoZeroFourEightBoard.TILE_SIZE);
						Main.fillRoundedRectangle(tileView, MapColorPalette.getColor(205, 193, 181));
					}
				}
			} else {
				MapFont<Character> titleFont = MapFont.MINECRAFT, subtitleFont = MapFont.TINY;
				String titleText = "2048", subtitleText = "TAP TO START";
				byte textColor = MapColorPalette.getColor(229, 198, 67);
				byte subColor = MapColorPalette.getSpecular(textColor, 0.7f);

				Dimension titleDimensions = this.view.calcFontSize(titleFont, titleText);
				Dimension subtitleDimensions = this.view.calcFontSize(subtitleFont, subtitleText);

				int titleX = (this.getWidth() - titleDimensions.width) / 2;
				int titleY = this.getHeight() / 2 - 4 - titleDimensions.height;
				int subtitleX = (this.getWidth() - subtitleDimensions.width) / 2;
				int subtitleY = this.getHeight() / 2 + 4;

				this.view.draw(titleFont, titleX + 1, titleY + 1, subColor, titleText);
				this.view.draw(subtitleFont, subtitleX + 1, subtitleY + 1, subColor, subtitleText);
				this.view.draw(titleFont, titleX, titleY, textColor, titleText);
				this.view.draw(subtitleFont, subtitleX, subtitleY, textColor, subtitleText);
			}
		}

		/**
		 * Reloads the widget
		 * <p>
		 * What we do here is:
		 * <ol>
		 * <li>Clear all existing widgets
		 * <li>Load the required board state
		 * <li>Draw the board to the root widget
		 * </ol>
		 *
		 * @see {@link MapWidget#clearWidgets()} for clearing all widgets
		 * @see {@link #loadBoard()} for loading the required board state
		 * @see {@link #drawBoard()} for drawing the board to the root widget
		 */
		public void reload() {
			if (this.display == null) {
				return;
			}
			this.clearWidgets();
			this.loadBoard();
			this.drawBoard();
		}

		/**
		 * Loads the required state of the board
		 * <p>
		 * This method does different things depending on the current state of the game.
		 * <table border="1">
		 * <tr>
		 * <th>{@link GameState}
		 * <th>Action
		 * <tr>
		 * <td>{@code TITLE}
		 * <td>Resets the tiles list
		 * <tr>
		 * <td>{@code GAME}
		 * <td>Generates a new board and sets the score to 0 if no tiles are generated;
		 * does nothing otherwise
		 * <tr>
		 * <td>{@code WIN} or {@code LOSE}
		 * <td>Opens the win/lose dialog
		 * </table>
		 *
		 * @see {@link #generateNewBoard()} for more information on how a new board is
		 *      generated
		 * @see {@link #openWinLoseDialog(boolean)} for more information on how the
		 *      win/lose dialog is opened
		 */
		private void loadBoard() {
			switch (this.state) {
			case TITLE: {
				this.tiles = null;
				break;
			}
			case GAME: {
				if (this.tiles == null) {
					// New Game
					this.generateNewBoard();
					this.score = 0;
				}
				break;
			}
			case WIN: {
				this.openWinLoseDialog(true);
				break;
			}
			case LOSE: {
				this.openWinLoseDialog(false);
				break;
			}
			}
		}

		/**
		 * Draws the board to the current root widget
		 * <p>
		 * This method adds tiles for each in the list.
		 */
		private void drawBoard() {
			if (this.tiles == null) {
				return;
			}

			for (TwoZeroFourEightTile tile : this.tiles) {
				if (tile != null) {
					this.addWidget(tile);
				}
			}

			if (this.state == GameState.GAME) {
				// Add 4 dummy map widgets that handle click
				int divideFactor = 4;
				int widthDivided = this.getWidth() / divideFactor, heightDivided = this.getHeight() / divideFactor;
				this.addWidget(new MapWidgetClickKey(MapPlayerInput.Key.UP) {
					@Override
					public void onClick(Key key) {
						TwoZeroFourEightBoard.this.handleMove(key);
					}
				}).setBounds(widthDivided, 0, widthDivided * (divideFactor - 2), heightDivided);

				this.addWidget(new MapWidgetClickKey(MapPlayerInput.Key.DOWN) {
					@Override
					public void onClick(Key key) {
						TwoZeroFourEightBoard.this.handleMove(key);
					}
				}).setBounds(widthDivided, heightDivided * (divideFactor - 1), widthDivided * (divideFactor - 2),
						heightDivided);

				this.addWidget(new MapWidgetClickKey(MapPlayerInput.Key.LEFT) {
					@Override
					public void onClick(Key key) {
						TwoZeroFourEightBoard.this.handleMove(key);
					}
				}).setBounds(0, heightDivided, widthDivided, heightDivided * (divideFactor - 2));

				this.addWidget(new MapWidgetClickKey(MapPlayerInput.Key.RIGHT) {
					@Override
					public void onClick(Key key) {
						TwoZeroFourEightBoard.this.handleMove(key);
					}
				}).setBounds(widthDivided * (divideFactor - 1), heightDivided, widthDivided,
						heightDivided * (divideFactor - 2));
			}
		}

		/**
		 * Generates a new board
		 * <p>
		 * In here, we do 3 things:
		 * <ol>
		 * <li>Generate all the required tiles, setting their bounds and focusable
		 * state, and hook into any callback functions that are required
		 * <li>Randomly pick tiles to generate the required number of mines. This loops
		 * infinitely until the required number is achieved
		 * <li>For each non-mine, count the number of mines surrounding it, and update
		 * the number that's shown on the tile
		 */
		private void generateNewBoard() {
			this.tiles = Arrays.asList(new TwoZeroFourEightTile[16]);

			for (int i = 0; i < 2; i++) {
				int index = this.findRandomEmptyTile();
				int value = TwoZeroFourEightBoard.getRandomStartingValue();

				TwoZeroFourEightTile tile = new TwoZeroFourEightTile(value);
				Point rowCol = this.getRowColFromIndex(index);
				tile.setBounds(this.getPixelPosFromIndex(rowCol.y), this.getPixelPosFromIndex(rowCol.x),
						TwoZeroFourEightBoard.TILE_SIZE, TwoZeroFourEightBoard.TILE_SIZE);
				this.tiles.set(index, tile);
			}
		}

		/**
		 * Handles the logic when a tile is uncovered
		 * <p>
		 * Read the comments that are inserted within the code to understand how this
		 * works.
		 */
		private void handleMove(MapPlayerInput.Key direction) {
			if (this.tiles.stream().filter(Objects::nonNull).anyMatch(TwoZeroFourEightTile::isMoving)) {
				return;
			}

			// Choose the order according to move direction
			int[] order = new int[0];

			switch (direction) {
			case UP:
				order = IntStream.range(0, TwoZeroFourEightBoard.BOARD_ROWS * TwoZeroFourEightBoard.BOARD_COLS)
						.toArray();
				break;
			case DOWN:
				order = IntStream.range(0, TwoZeroFourEightBoard.BOARD_ROWS * TwoZeroFourEightBoard.BOARD_COLS)
						.map(i -> ~i).sorted().map(i -> ~i).toArray();
				break;
			case LEFT:
				order = IntStream.range(0, TwoZeroFourEightBoard.BOARD_COLS).flatMap(i -> IntStream
						.range(0, TwoZeroFourEightBoard.BOARD_ROWS).map(j -> j * TwoZeroFourEightBoard.BOARD_COLS + i))
						.toArray();
				break;
			case RIGHT:
				order = IntStream.range(0, TwoZeroFourEightBoard.BOARD_COLS).map(i -> ~i).sorted().map(i -> ~i)
						.flatMap(i -> IntStream.range(0, TwoZeroFourEightBoard.BOARD_ROWS)
								.map(j -> j * TwoZeroFourEightBoard.BOARD_COLS + i))
						.toArray();
				break;
			default:
				break;
			}

			// For each tile, move to position, merging when necessary
			boolean hasMoved = false;
			for (int index : order) {
				TwoZeroFourEightTile current = this.getTile(index);
				if (current == null) {
					continue;
				}

				Point rowCol = this.getRowColFromIndex(index);
				int row = rowCol.x, col = rowCol.y;

				switch (direction) {
				case UP: {
					while (row >= 0) {
						int newRow = row - 1, newCol = col;
						boolean nextToWall = newRow < 0;
						TwoZeroFourEightTile adjacent = this.getTile(newRow, newCol);
						if (nextToWall || adjacent != null && (adjacent.hasBufferedValue() || current.hasBufferedValue()
								|| adjacent.getValue() != current.getValue())) {
							// It is next to wall or tile of different value. End movement on current tile.
							this.moveTile(index, row, col);
							break;
						}
						if (adjacent != null && adjacent.getValue() == current.getValue()) {
							// It is next to tile of same value. End movement on next tile.
							this.mergeScores(current, adjacent);
							hasMoved = true;
							this.moveTile(index, newRow, newCol);
							break;
						}
						// There is room to move
						row = newRow;
						col = newCol;
						hasMoved = true;
					}
					break;
				}
				case DOWN: {
					while (row < TwoZeroFourEightBoard.BOARD_ROWS) {
						int newRow = row + 1, newCol = col;
						boolean nextToWall = newRow >= TwoZeroFourEightBoard.BOARD_ROWS;
						TwoZeroFourEightTile adjacent = this.getTile(newRow, newCol);
						if (nextToWall || adjacent != null && (adjacent.hasBufferedValue() || current.hasBufferedValue()
								|| adjacent.getValue() != current.getValue())) {
							// It is next to wall or tile of different value. End movement on current tile.
							this.moveTile(index, row, col);
							break;
						}
						if (adjacent != null && adjacent.getValue() == current.getValue()) {
							// It is next to tile of same value. End movement on next tile.
							this.mergeScores(current, adjacent);
							hasMoved = true;
							this.moveTile(index, newRow, newCol);
							break;
						}
						// There is room to move
						row = newRow;
						col = newCol;
						hasMoved = true;
					}
					break;
				}
				case LEFT: {
					while (col >= 0) {
						int newRow = row, newCol = col - 1;
						boolean nextToWall = newCol < 0;
						TwoZeroFourEightTile adjacent = this.getTile(newRow, newCol);
						if (nextToWall || adjacent != null && (adjacent.hasBufferedValue() || current.hasBufferedValue()
								|| adjacent.getValue() != current.getValue())) {
							// It is next to wall or tile of different value. End movement on current tile.
							this.moveTile(index, row, col);
							break;
						}
						if (adjacent != null && adjacent.getValue() == current.getValue()) {
							// It is next to tile of same value. End movement on next tile.
							this.mergeScores(current, adjacent);
							hasMoved = true;
							this.moveTile(index, newRow, newCol);
							break;
						}
						// There is room to move
						row = newRow;
						col = newCol;
						hasMoved = true;
					}
					break;
				}
				case RIGHT: {
					while (col < TwoZeroFourEightBoard.BOARD_COLS) {
						int newRow = row, newCol = col + 1;
						boolean nextToWall = newCol >= TwoZeroFourEightBoard.BOARD_COLS;
						TwoZeroFourEightTile adjacent = this.getTile(newRow, newCol);
						if (nextToWall || adjacent != null && (adjacent.hasBufferedValue() || current.hasBufferedValue()
								|| adjacent.getValue() != current.getValue())) {
							// It is next to wall or tile of different value. End movement on current tile.
							this.moveTile(index, row, col);
							break;
						}
						if (adjacent != null && adjacent.getValue() == current.getValue()) {
							// It is next to tile of same value. End movement on next tile.
							this.mergeScores(current, adjacent);
							hasMoved = true;
							this.moveTile(index, newRow, newCol);
							break;
						}
						// There is room to move
						row = newRow;
						col = newCol;
						hasMoved = true;
					}
					break;
				}
				default:
					break;
				}
			}

			if (hasMoved) {
				this.movementCounter = TwoZeroFourEightBoard.MOVEMENT_FRAMES;
			}
		}

		/**
		 * Moves a tile to the new location in the list, updating its position on the
		 * display
		 *
		 * @param oldIndex the current index of the tile
		 * @param newRow   the new row number of the tile
		 * @param newCol   the new column number of the tile
		 */
		private void moveTile(int oldIndex, int newRow, int newCol) {
			TwoZeroFourEightTile tile = this.tiles.get(oldIndex);
			tile.setTargetPosition(this.getPixelPosFromIndex(newCol), this.getPixelPosFromIndex(newRow));
			this.tiles.set(oldIndex, null);
			this.tiles.set(this.getIndexFromRowCol(newRow, newCol), tile);
		}

		/**
		 * Merge the scores of two tiles
		 *
		 * @param keep the tile to keep
		 * @param lose the tile to lose
		 */
		private void mergeScores(TwoZeroFourEightTile keep, TwoZeroFourEightTile lose) {
			int newValue = keep.getValue() + lose.getValue();
			keep.bufferValue(newValue);
			lose.bufferValue(-1);
			this.score += newValue;
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * We use this method to open the difficulty selector when a left-click action
		 * is received and there are no child widgets on the board.
		 * <p>
		 * It is important that the status event is sent to the {@code MapWidget} parent
		 * ({@code super}) if the event is not handled, otherwise it will not propagate.
		 */
		@Override
		public void onStatusChanged(MapStatusEvent event) {
			if (this.getWidgetCount() == 0 && event.getName().equals("LEFT_CLICK")) {
				this.setState(GameState.GAME);
				return;
			}

			super.onStatusChanged(event);
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * We use this method to open the difficulty selector when the {@code ENTER} key
		 * is received and there are no child widgets on the board.
		 * <p>
		 * It is important that the key press event is sent to the {@code MapWidget}
		 * parent ({@code super}) if the event is not handled, otherwise it will not
		 * propagate.
		 */
		@Override
		public void onKeyPressed(MapKeyEvent event) {
			if (this.getWidgetCount() == 0 && event.getKey() == MapPlayerInput.Key.ENTER) {
				this.setState(GameState.GAME);
				return;
			}
			if (this.state == GameState.GAME && this.tiles != null) {
				this.handleMove(event.getKey());
				return;
			}

			super.onKeyPressed(event);
		}

		/**
		 * Opens the win/lose dialog
		 * <p>
		 * This method first sets all board tiles to be non-focusable, then creates the
		 * win/lose dialog, passing any arguments as needed and hooking into any
		 * callback methods as required, and adds it as a widget to the board.
		 *
		 * @param win whether the game ended in a win or not
		 * @see TwoZeroFourEightWinLoseDialog
		 */
		private void openWinLoseDialog(boolean win) {
			this.addWidget(new TwoZeroFourEightWinLoseDialog(win, this.score) {
				@Override
				public void onClose() {
					if (this.keepGoing) {
						TwoZeroFourEightBoard.this.continueMode = true;
						TwoZeroFourEightBoard.this.setState(GameState.GAME);
					} else {
						TwoZeroFourEightBoard.this.setState(GameState.TITLE);
					}
				}
			});
		}

		/**
		 * Sets the current game state
		 * <p>
		 * Setting the game state triggers a board reload.
		 *
		 * @param newState the new game state
		 */
		public void setState(GameState newState) {
			this.state = newState;
			this.reload();
		}

		/**
		 * Gets a tile from its row,col location
		 *
		 * @param row the row of the tile
		 * @param col the column of the tile
		 * @return the requested tile, or null if there are no tiles or the selection is
		 *         out of bounds
		 */
		private TwoZeroFourEightTile getTile(int row, int col) {
			if (this.tiles == null) {
				return null;
			}
			if (row < 0 || row >= TwoZeroFourEightBoard.BOARD_ROWS || col < 0
					|| col >= TwoZeroFourEightBoard.BOARD_COLS) {
				return null;
			}
			return this.tiles.get(row * TwoZeroFourEightBoard.BOARD_COLS + col);
		}

		/**
		 * Gets a tile from its index
		 *
		 * @param index the index of the tile
		 * @return the requested tile, or null if there are no tiles or the selection is
		 *         out of bounds
		 */
		private TwoZeroFourEightTile getTile(int index) {
			if (this.tiles == null) {
				return null;
			}
			return this.tiles.get(index);
		}

		/**
		 * Calculates the row and column of a tile with a given index
		 *
		 * @param index the index of the tile
		 * @return the row and column of the tile
		 */
		private Point getRowColFromIndex(int index) {
			if (index < 0 || index >= this.tiles.size()) {
				return null;
			}
			return new Point(index / TwoZeroFourEightBoard.BOARD_COLS, index % TwoZeroFourEightBoard.BOARD_COLS);
		}

		private int getIndexFromRowCol(int row, int col) {
			if (row < 0 || row >= TwoZeroFourEightBoard.BOARD_ROWS || col < 0
					|| col >= TwoZeroFourEightBoard.BOARD_COLS) {
				return -1;
			}
			return row * TwoZeroFourEightBoard.BOARD_COLS + col;
		}

		/**
		 * Gets the pixel position from the row/column index
		 *
		 * @param index the row/column index
		 * @return the pixel position
		 */
		private int getPixelPosFromIndex(int index) {
			return index * TwoZeroFourEightBoard.TILE_OFFSET + TwoZeroFourEightBoard.TOP_LEFT_TILE;
		}

		private int findRandomEmptyTile() {
			if (!this.tiles.contains(null)) {
				return -1;
			}
			while (true) {
				int index = TwoZeroFourEightBoard.RANDOM.nextInt(this.tiles.size());
				if (this.tiles.get(index) == null) {
					return index;
				}
			}
		}

		private static int getRandomStartingValue() {
			return TwoZeroFourEightBoard.RANDOM.nextDouble() <= 0.1 ? 4 : 2;
		}
	}

	/**
	 * This is the tile widget class for the 2048 game
	 * <p>
	 * This is a {@link MapWidget} class. Use this class to understand how to use
	 * {@code MapWidget}s.
	 * <p>
	 * This widget handles the appearance and visuals for each 2048 tile. The game
	 * logic is handled in a different widget.
	 * <p>
	 * This class can have any visibility, as long as it is visible to the display
	 * class.
	 *
	 * @author Cyrus Mian Xi Li (bbayu/bbayu123)
	 *
	 * @see {@link TwoZeroFourEightBoard} for the game logic widget
	 */
	private static class TwoZeroFourEightTile extends MapWidget {
		/**
		 * The numeric value of a tile
		 */
		private int value = 0;

		private int targetX = -1;
		private int targetY = -1;
		private int rateOfMoveX = 0;
		private int rateOfMoveY = 0;
		private int bufferedValue = 0;
		private int bufferCounter = 0;

		/**
		 *
		 * @param value
		 */
		public TwoZeroFourEightTile(int value) {
			this.value = value;
		}

		@Override
		public void onTick() {
			// If the tile has reached the target position, reset target to -1
			if (this.targetX != -1 && this.targetX == this.getX()) {
				this.targetX = -1;
				this.rateOfMoveX = 0;
			}
			if (this.targetY != -1 && this.targetY == this.getY()) {
				this.targetY = -1;
				this.rateOfMoveY = 0;
			}

			// If rate of move is larger than distance, shorten rate of move
			if (Math.abs(this.rateOfMoveX) > Math.abs(this.targetX - this.getX())) {
				this.rateOfMoveX = this.targetX - this.getX();
			}
			if (Math.abs(this.rateOfMoveY) > Math.abs(this.targetY - this.getY())) {
				this.rateOfMoveY = this.targetY - this.getY();
			}

			// If the rate of move is not zero, move to the target
			int x = this.getX(), y = this.getY();
			if (this.rateOfMoveX != 0) {
				x += this.rateOfMoveX;
			}
			if (this.rateOfMoveY != 0) {
				y += this.rateOfMoveY;
			}
			this.setPosition(x, y);

			// If the target is set but there is no movement, set rate of movement
			if (this.targetX != -1 && this.rateOfMoveX == 0) {
				this.rateOfMoveX = (this.targetX - this.getX()) / TwoZeroFourEightBoard.MOVEMENT_FRAMES;
			}
			if (this.targetY != -1 && this.rateOfMoveY == 0) {
				this.rateOfMoveY = (this.targetY - this.getY()) / TwoZeroFourEightBoard.MOVEMENT_FRAMES;
			}

			// Update buffered value
			if (this.bufferCounter > 0) {
				if (--this.bufferCounter == 0) {
					this.applyBufferedValue();
				}
			}
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * Here, the drawing routine is as follows:
		 * <ol>
		 * <li>Draw a rectangle border that is the size of the tile
		 * <li>If the tile is covered:
		 * <ol type="a">
		 * <li>Fill the area with the cover color
		 * <li>Add the flag icon if the tile is flagged. The flag icon is obtained from
		 * the {@link Main} class.
		 * </ol>
		 * Otherwise:
		 * <ol type="a">
		 * <li>Fill the area with the background color
		 * <li>Add an icon/character representing the tile, either a mine, a number, or
		 * nothing if no mines surround the tile
		 * </ol>
		 * </ol>
		 *
		 * @see {@link MapColorPalette#getColor(int, int, int)} for getting a byte color
		 *      from RGB
		 * @see {@link MapWidget#view} for obtaining the canvas that is bound by the
		 *      widget
		 * @see {@link MapCanvas#drawRectangle(int, int, int, int, byte)} for drawing a
		 *      bordered rectangle
		 * @see {@link MapCanvas#fillRectangle(int, int, int, int, byte)} for drawing a
		 *      filled rectangle
		 * @see {@link MapCanvas#draw(MapCanvas, int, int)} for drawing a
		 *      {@code MapCanvas} or {@link MapTexture}
		 * @see {@link MapCanvas#draw(MapFont, int, int, byte, CharSequence)} for
		 *      drawing text using a given {@link MapFont}
		 * @see {@link MapWidget#display} for obtaining the display that this widget is
		 *      attached to
		 * @see {@link MapDisplay#getPlugin()} for obtaining the {@code JavaPlugin} of
		 *      the display
		 */
		@Override
		public void onDraw() {
			byte[] backColors = new byte[] { MapColorPalette.getColor(236, 228, 219),
					MapColorPalette.getColor(235, 227, 207), MapColorPalette.getColor(234, 180, 132),
					MapColorPalette.getColor(233, 155, 115), MapColorPalette.getColor(231, 132, 111),
					MapColorPalette.getColor(230, 107, 82), MapColorPalette.getColor(234, 214, 153),
					MapColorPalette.getColor(233, 213, 142), MapColorPalette.getColor(240, 213, 113),
					MapColorPalette.getColor(232, 207, 122), MapColorPalette.getColor(229, 198, 67),
					MapColorPalette.getColor(244, 102, 116), MapColorPalette.getColor(241, 75, 97),
					MapColorPalette.getColor(235, 66, 63), MapColorPalette.getColor(113, 179, 218),
					MapColorPalette.getColor(94, 160, 230), MapColorPalette.getColor(2, 125, 192) };

			byte[] textColors = new byte[] { MapColorPalette.getColor(118, 111, 100),
					MapColorPalette.getColor(251, 247, 241) };

			// Background
			int backColorIndex = (int) Math.round(Math.log(this.value) / Math.log(2)) - 1;
			Main.fillRoundedRectangle(this.view, backColors[backColorIndex]);

			// Text
			MapFont<Character> font = MapFont.MINECRAFT;
			Dimension dimensions = this.view.calcFontSize(font, String.valueOf(this.value));
			if (dimensions.width > this.getWidth() - 4) {
				font = MapFont.TINY;
				dimensions = this.view.calcFontSize(font, String.valueOf(this.value));
			}

			int textX = (this.getWidth() - dimensions.width) / 2 + 1;
			int textY = (this.getHeight() - dimensions.height) / 2 + 1;
			this.view.draw(font, textX, textY, textColors[this.value > 4 ? 1 : 0], String.valueOf(this.value));
		}

		/**
		 * Gets the numeric value shown on the tile
		 *
		 * @return the numeric value on the tile
		 */
		public int getValue() {
			return this.value;
		}

		/**
		 * Sets the numeric value shown on the tile
		 *
		 * @param value the new numeric value of the tile
		 */
		public void setValue(int value) {
			this.value = value;
			this.invalidate();
		}

		public void setTargetPosition(int targetX, int targetY) {
			if (targetX != this.getX()) {
				this.targetX = targetX;
			}
			if (targetY != this.getY()) {
				this.targetY = targetY;
			}
		}

		public boolean hasBufferedValue() {
			return this.bufferedValue != 0;
		}

		public void bufferValue(int value) {
			this.bufferedValue = value;
			this.bufferCounter = TwoZeroFourEightBoard.MOVEMENT_FRAMES;
		}

		private void applyBufferedValue() {
			if (this.bufferedValue > 0) {
				this.setValue(this.bufferedValue);
				this.bufferedValue = 0;
			} else if (this.bufferedValue < 0) {
				this.removeWidget();
			}
		}

		public boolean isMoving() {
			return this.targetX != -1 || this.targetY != -1 || this.rateOfMoveX != 0 || this.rateOfMoveY != 0;
		}
	}

	/**
	 * This is the game end dialog class for the 2048 game
	 * <p>
	 * This is a {@link MapWidgetWindow} class. Use this class to understand how to
	 * use {@code MapWidgetWindow}s.
	 * <p>
	 * This widget shows when the game has ended.
	 * <p>
	 * This class can have any visibility, as long as it is visible to the display
	 * class.
	 *
	 * @author Cyrus Mian Xi Li (bbayu/bbayu123)
	 */
	private static class TwoZeroFourEightWinLoseDialog extends MapWidgetWindow {
		/**
		 * Whether the game ended in a win or not
		 */
		private final boolean win;
		/**
		 * The score of the game
		 */
		private final int score;

		/**
		 * The "Keep Going" button widget
		 */
		private MapWidget contButton = null;

		/**
		 * The "Back to Title" button widget
		 */
		private MapWidget button = null;

		/**
		 * Whether to keep going or not
		 */
		protected boolean keepGoing = false;

		/**
		 * Creates the dialog
		 *
		 * @param win   whether the game ended in a win or not
		 * @param time  the time from start to end
		 * @param flags a pair of values representing the number of flags placed and the
		 *              number of mines on the board
		 */
		public TwoZeroFourEightWinLoseDialog(boolean win, int score) {
			this.win = win;
			this.score = score;

			this.setBounds(15, 22, 95, 58);
			this.setBackgroundColor(MapColorPalette.getColor(114, 121, 175));
			this.setDepthOffset(4);
			this.setFocusable(true);
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * In here, we activate the dialog window, then add the required widgets,
		 * setting up any behavior that is needed.
		 */
		@Override
		public void onAttached() {
			super.onAttached();
			this.activate();

			// Label
			this.addWidget(new MapWidgetText().setText(this.win ? "You Win!" : "Game Over").setBounds(5, 5, 80, 13));

			// Score
			this.addWidget(
					new MapWidgetText().setText(String.format("Score: %d", this.score)).setBounds(5, 17, 80, 13));

			if (this.win) {
				// Continue
				this.contButton = this.addWidget(new MapWidgetButton() {
					@Override
					public void onActivate() {
						TwoZeroFourEightWinLoseDialog.this.keepGoing = true;
						TwoZeroFourEightWinLoseDialog.this.close();
					}
				}.setText("Keep\ngoing").setBounds(10, 33, 35, 20));

				// Button
				this.button = this.addWidget(new MapWidgetButton() {
					@Override
					public void onActivate() {
						TwoZeroFourEightWinLoseDialog.this.close();
					}
				}.setText("Back\nto title").setBounds(45, 33, 35, 20));
			} else {
				// Button
				this.button = this.addWidget(new MapWidgetButton() {
					@Override
					public void onActivate() {
						TwoZeroFourEightWinLoseDialog.this.close();
					}
				}.setText("Back to title").setBounds(10, 40, 70, 13));
			}
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * If the {@code BACK} key is pressed at any time while the dialog is activated,
		 * close it.
		 * <p>
		 * It is important that the key press event is sent to the {@code MapWidget}
		 * parent ({@code super}) if the event is not handled, otherwise it will not
		 * propagate.
		 *
		 * @param event the key press event that was received
		 */
		@Override
		public void onKeyPressed(MapKeyEvent event) {
			if (event.getKey() == MapPlayerInput.Key.BACK && this.isActivated()) {
				this.close();
				return;
			}
			super.onKeyPressed(event);
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * When the correct mouse-related status event is received, the current widget
		 * is activated, and the mouse location is within bounds of the widget:
		 * <p>
		 * <ul>
		 * <li>If it is a hover, focus the hovered child widget
		 * <li>If it is a left-click, focus and activate the child widget
		 * </ul>
		 * <p>
		 * It is important that the status event is sent to the {@code MapWidget} parent
		 * ({@code super}) if the event is not handled, otherwise it will not propagate.
		 *
		 * @see {@link Main#isInBounds(MapWidget, int, int)} for more information on how
		 *      the in-bounds check is done
		 */
		@Override
		public void onStatusChanged(MapStatusEvent event) {
			if (!this.isActivated() || !Arrays.asList("LEFT_CLICK", "RIGHT_CLICK", "HOVER").contains(event.getName())) {
				return;
			}
			Point clicked = event.getArgument(Point.class);
			int clickedX = clicked.x, clickedY = clicked.y;

			if (!Main.isInBounds(this, clickedX, clickedY)) {
				return;
			}

			if (event.getName().equals("HOVER")) {
				if (Main.isInBounds(this.contButton, clickedX, clickedY)) {
					this.contButton.focus();
				} else if (Main.isInBounds(this.button, clickedX, clickedY)) {
					this.button.focus();
				}
			} else if (event.getName().equals("LEFT_CLICK")) {
				if (Main.isInBounds(this.contButton, clickedX, clickedY)) {
					this.contButton.focus();
					this.contButton.activate();
				} else if (Main.isInBounds(this.button, clickedX, clickedY)) {
					this.button.focus();
					this.button.activate();
				}
			}
		}

		/**
		 * Closes the dialog
		 */
		public void close() {
			this.removeWidget();
			this.onClose();
		}

		/**
		 * Callback after the dialog has been closed
		 * <p>
		 * This method should be overridden to handle the post-close action.
		 */
		public void onClose() {
		}
	}

	/**
	 * This represents the game state of the Minesweeper game
	 *
	 * @author Cyrus Mian Xi Li (bbayu/bbayu123)
	 */
	private enum GameState {
		/**
		 * The state while on the title
		 */
		TITLE,
		/**
		 * The state while in-game
		 */
		GAME,
		/**
		 * The state while the game-ended in a win
		 */
		WIN,
		/**
		 * The state while the game-ended in a lose
		 */
		LOSE
	}
}

class MapWidgetClickKey extends MapWidget {
	private final MapPlayerInput.Key moveDirection;

	public MapWidgetClickKey(MapPlayerInput.Key moveDirection) {
		this.moveDirection = moveDirection;
	}

	@Override
	public void onStatusChanged(MapStatusEvent event) {
		if (this.display != null && event.getName().equals("LEFT_CLICK")) {
			Point clicked = event.getArgument(Point.class);
			if (Main.isInBounds(this, clicked.x, clicked.y)) {
				this.onClick(this.moveDirection);
				return;
			}
		}

		super.onStatusChanged(event);
	}

	public void onClick(MapPlayerInput.Key key) {
	}
}

/**
 * This represents a pair of the same object type
 *
 * @author Cyrus Mian Xi Li (bbayu/bbayu123)
 */
class Pair<T> {
	private final T first, second;

	public Pair(T first, T second) {
		this.first = first;
		this.second = second;
	}

	public T first() {
		return this.first;
	}

	public T second() {
		return this.second;
	}
}
