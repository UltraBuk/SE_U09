/*  License (BSD Style License):
 *  Copyright (c) 2010
 *  Software Engineering
 *  Department of Computer Science
 *  Technische Universität Darmstadt
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *  - Neither the name of the Software Engineering Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 *  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 *  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 */
package de.tud.cs.se.flashcards.ui;

import static de.tud.cs.se.flashcards.ui.Utilities.createToolBarButton;
import static java.awt.Toolkit.getDefaultToolkit;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.INFORMATION_MESSAGE;
import static javax.swing.JOptionPane.NO_OPTION;
import static javax.swing.JOptionPane.WARNING_MESSAGE;
import static javax.swing.JOptionPane.YES_NO_OPTION;
import static javax.swing.JOptionPane.showConfirmDialog;
import static javax.swing.JOptionPane.showMessageDialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import de.tud.cs.se.flashcards.model.Command;
import de.tud.cs.se.flashcards.model.CommandHistory;
import de.tud.cs.se.flashcards.model.DefaultFlashcardSeries;
import de.tud.cs.se.flashcards.model.Flashcard;
import de.tud.cs.se.flashcards.model.FlashcardObserver;
import de.tud.cs.se.flashcards.model.FlashcardSeries;
import de.tud.cs.se.flashcards.model.FlashcardSeriesFilter;
import de.tud.cs.se.flashcards.model.SortedFlashcardSeries;
import de.tud.cs.se.flashcards.model.learning.LearningStrategies;
import de.tud.cs.se.flashcards.model.learning.LearningStrategy;
import de.tud.cs.se.flashcards.persistence.Store;
import de.tud.cs.st.constraints.NotNull;
import de.tud.cs.st.constraints.Null;


/**
 * A Frame is always associated with exactly one document and it is the parent of all related
 * dialogs etc.
 *
 * @author Michael Eichberg
 */
public final class FlashcardsEditor {

    // The UI components:

    private final JFrame frame;

    private final JButton removeButton;


    private final JButton editButton;


    private final JButton undoButton;


    private final JButton redoButton;


    private final JButton playButton;

    private final JButton sortOrderButton;


    private final JPopupMenu sortOrderPopupMenu;


    private final Dimension sortOrderPopupMenuDimension;


    private final JList<Flashcard> list;

    private final FlashcardEditor flashcardEditor;


    private final LearnDialog learnDialog;


    private final FileDialog fileDialog;


    private final DateTimeFormatter infoPaneDateFormatter =
            DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);


    private final JLabel dateCreatedLabel;


    private final JLabel lastTimeNotRemeberedLabel;


    private final JLabel lastTimeRememberedLabel;


    private final JLabel numberOfTimesShownLabel;


    private final JLabel numberOfTimesRememberedInARowLabel;


    private final FlashcardObserver infoPaneUpdateObserver = new FlashcardObserver() {

        public void cardChanged(Flashcard flashcard) {

            updateInfoPaneLabels(list.getSelectedValuesList());
        }
    };


    private final JTextField searchTextField;


    // Document related state of the editor:

    private final CommandHistory commands;


    private final FlashcardSeries flashcards;


    private final FlashcardSeriesFilter flashcardSeriesFilter;


    private final SortedFlashcardSeries sortedFlashcards;


    private File file;


    private Flashcard infoPaneFlashcard = null;


    private boolean documentChanged = false;


    // Factory method(s):

    public static boolean createFlashcardsEditor(@NotNull File file) {

        try {
            new FlashcardsEditor(file);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            showMessageDialog(
                    null,
                    "The document \"" + file.getName() + "\" could not be read.",
                    "",
                    WARNING_MESSAGE);
            return false;
        }
    }


    // The instance methods:

    private FlashcardsEditor(
            @NotNull
                    File file
    ) throws IOException {

        this(Store.openSeries(file));

        this.file = file;
        Utilities.setFrameTitle(frame, file);
    }


    private FlashcardsEditor(
            @NotNull
                    FlashcardSeries flashcards
    ) {

        /*
         * General Design Decision(s):
         *
         * ActionListener do not contain domain logic; they always delegate to corresponding methods.
         *
         * All errors are handled as early as possible.
         *
         * Each (Document)Frame is associated with exactly one FlashcardSeries.
         */

        this.flashcards = flashcards;
        this.flashcardSeriesFilter = new FlashcardSeriesFilter(flashcards);
        this.sortedFlashcards = new SortedFlashcardSeries(flashcardSeriesFilter);

        this.commands = new CommandHistory();

        // setup of this frame; we need to do it here since the rootpane's
        // client property has to be set before the other components are created
        frame = new JFrame();
        frame.getRootPane().putClientProperty("apple.awt.brushMetalLook", java.lang.Boolean.TRUE);
        Utilities.setFrameTitle(frame, null);

        // Dialogs and other components that are related with this frame:

        FlashcardListCellRenderer flashcardListCellRenderer = new FlashcardListCellRenderer();

        flashcardEditor = new FlashcardEditor(this);

        learnDialog = new LearnDialog(this);

        fileDialog = new java.awt.FileDialog(frame);
        fileDialog.setFilenameFilter((directory, name) -> name.endsWith(Store.FILE_ENDING));

        // Setup the menu and its listeners:
        JMenuBar menuBar = setupMenuBar();

        // Setup of the toolbar:
        removeButton = createToolBarButton(
                " Delete ",
                "list-remove.png",
                "remove selected flashcards");
        editButton = createToolBarButton(
                " Edit ",
                "accessories-text-editor.png",
                "edit selected flashcard");
        undoButton = createToolBarButton(" Undo ", "edit-undo.png", "undos the last action");
        redoButton = createToolBarButton(" Redo ", "edit-redo.png", "redos the last action");
        playButton = createToolBarButton(" Learn ", "media-playback-start.png", "learn flashcards");

        searchTextField = new JTextField(8);

        JToolBar toolbar = setupToolbar();

        JPanel infoPanel = new JPanel();
        infoPanel.setOpaque(true);
        infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        infoPanel.setBackground(new Color(220, 220, 250));
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.PAGE_AXIS));
        dateCreatedLabel = createInfoPaneLabel(infoPanel, "Date Created:");
        lastTimeNotRemeberedLabel = createInfoPaneLabel(infoPanel, "Last Time Not Remembered:");
        lastTimeRememberedLabel = createInfoPaneLabel(infoPanel, "Last Time Remembered:");
        numberOfTimesShownLabel = createInfoPaneLabel(infoPanel, "Number of Times Shown:");
        numberOfTimesRememberedInARowLabel = createInfoPaneLabel(
                infoPanel,
                "Number of Times Remembered in a Row:");

        sortOrderPopupMenu = new JPopupMenu("Sort Order");
        sortOrderPopupMenuDimension = sortOrderPopupMenu.getPreferredSize();

        sortOrderButton = new JButton("Date Created", Utilities.createImageIcon(
                "select.png",
                "Select"));
        setupSortOrderPopupMenu();

        // Setup of the main list
        list = new JList<>(sortedFlashcards);
        list.setCellRenderer(flashcardListCellRenderer);

        list.addListSelectionListener(event -> {

            // Only GUI related functionality:
            if (list.getSelectedIndex() != -1) {
                removeButton.setEnabled(true);
                editButton.setEnabled(true);
                updateInfoPaneLabels(list.getSelectedValuesList());
            } else {
                removeButton.setEnabled(false);
                editButton.setEnabled(false);
                updateInfoPaneLabels(null);
            }
        });

        JSplitPane splitPane = setupJSplitPane();
        splitPane.setRightComponent(infoPanel);
        // frame.setTransferHandler(new Tran)
        // frame.getContentPane().setDropTarget(null);

        frame.setJMenuBar(menuBar);
        frame.getContentPane().add(splitPane);
        frame.getContentPane().add(toolbar, BorderLayout.NORTH);
        frame.setSize(640, 480);
        frame.setLocationByPlatform(true);
        frame.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        addListeners(flashcards);

        // Everything is setup; show the window:
        frame.setVisible(true);
    }


    private JMenuBar setupMenuBar() {
        JMenuItem newFileMenuItem = new JMenuItem("New");
        newFileMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, getDefaultToolkit()
                .getMenuShortcutKeyMask()));
        newFileMenuItem.addActionListener(event -> newFlashcardEditor());

        JMenuItem openFileMenuItem = new JMenuItem("Open File...");
        openFileMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, getDefaultToolkit()
                .getMenuShortcutKeyMask()));
        openFileMenuItem.addActionListener(event -> openFlashcardSeries());

        JMenuItem saveFileMenuItem = new JMenuItem("Save");
        saveFileMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, getDefaultToolkit()
                .getMenuShortcutKeyMask()));
        saveFileMenuItem.addActionListener(event -> saveFlashcardSeries());

        JMenuItem saveAsFileMenuItem = new JMenuItem("Save As...");
        saveAsFileMenuItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_S,
                (InputEvent.SHIFT_MASK | getDefaultToolkit().getMenuShortcutKeyMask())));
        saveAsFileMenuItem.addActionListener(event -> saveAsFlashcardSeries());

        JMenuItem closeFileMenuItem = new JMenuItem("Close Window");
        closeFileMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, getDefaultToolkit()
                .getMenuShortcutKeyMask()));
        closeFileMenuItem.addActionListener(event -> closeFlashcardEditor());

        JMenu fileMenu = new JMenu("File");
        fileMenu.add(newFileMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(openFileMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(saveFileMenuItem);
        fileMenu.add(saveAsFileMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(closeFileMenuItem);

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(fileMenu);
        return menuBar;
    }


    private JToolBar setupToolbar() {
        JButton addButton = createToolBarButton(" Create ", "list-add.png", "create new flashcard");
        addButton.addActionListener(event -> createFlashcard());

        removeButton.setEnabled(false);
        removeButton.addActionListener(event -> removeFlashcards());

        editButton.setEnabled(false);
        editButton.addActionListener(event -> editFlashcard());

        undoButton.setEnabled(false);
        undoButton.addActionListener(e -> commands.undo());
        redoButton.setEnabled(false);
        redoButton.addActionListener(e -> commands.redo());
        commands.setCommandHistoryChangedListener(commands -> {

            if (commands.undoableCommandsCount() == 0)
                undoButton.setEnabled(false);
            else
                undoButton.setEnabled(true);

            if (commands.redoableCommandsCount() == 0)
                redoButton.setEnabled(false);
            else
                redoButton.setEnabled(true);

        });

        playButton.addActionListener(event -> learn());

        searchTextField.putClientProperty("JTextField.variant", "search");
        searchTextField.setMaximumSize(searchTextField.getPreferredSize());
        searchTextField.getDocument().addDocumentListener(new DocumentListener() {

            public void removeUpdate(DocumentEvent e) {

                flashcardSeriesFilter.setSearchTerm(searchTextField.getText());

            }


            public void insertUpdate(DocumentEvent e) {

                flashcardSeriesFilter.setSearchTerm(searchTextField.getText());

            }


            public void changedUpdate(DocumentEvent e) {

                flashcardSeriesFilter.setSearchTerm(searchTextField.getText());
            }
        });

        JToolBar toolbar = new JToolBar();
        toolbar.putClientProperty("JToolBar.isRollover", Boolean.FALSE); // required for Mac OS X
        toolbar.setRollover(false);
        toolbar.add(addButton);
        toolbar.add(removeButton);
        toolbar.addSeparator();
        toolbar.add(editButton);
        toolbar.addSeparator();
        toolbar.add(undoButton);
        toolbar.add(redoButton);

        toolbar.add(Box.createHorizontalGlue());
        toolbar.add(playButton);

        toolbar.add(Box.createHorizontalGlue());
        toolbar.add(searchTextField);
        toolbar.setFloatable(false);
        return toolbar;
    }


    private void setupSortOrderPopupMenu() {
        final JMenuItem dateCreatedMenuItem = new JMenuItem("Date Created");
        dateCreatedMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                sortOrderButton.setText(dateCreatedMenuItem.getText());
                FlashcardsEditor.this.sortedFlashcards
                        .setSortingStrategy(sortedFlashcards.dateCreatedStrategy());

            }
        });
        final JMenuItem lastTimeRemeberedMenuItem = new JMenuItem("Last Time Remembered");
        lastTimeRemeberedMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                sortOrderButton.setText(lastTimeRemeberedMenuItem.getText());
                FlashcardsEditor.this.sortedFlashcards.setSortingStrategy(sortedFlashcards.lastTimeRememberedStrategy());

            }

        });
        final JMenuItem rememberedInARowCountMenuItem = new JMenuItem(
                "Number of Times Remembered in a Row");
        rememberedInARowCountMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                sortOrderButton.setText(rememberedInARowCountMenuItem.getText());
                FlashcardsEditor.this.sortedFlashcards.setSortingStrategy(sortedFlashcards.timesRememberedInARowStrategy());

            }

        });

        sortOrderPopupMenu.add(dateCreatedMenuItem);
        sortOrderPopupMenu.add(lastTimeRemeberedMenuItem);
        sortOrderPopupMenu.add(rememberedInARowCountMenuItem);
        sortOrderButton.setIconTextGap(10);
        sortOrderButton.setVerticalTextPosition(SwingConstants.CENTER);
        sortOrderButton.setHorizontalTextPosition(SwingConstants.LEFT);
        sortOrderButton.putClientProperty("JButton.buttonType", "roundRect");
        sortOrderButton.putClientProperty("JComponent.sizeVariant", "small");
        sortOrderButton.addActionListener(
                e -> sortOrderPopupMenu.show(sortOrderButton, sortOrderButton.getWidth() / 2
                        - (sortOrderPopupMenuDimension.width / 2), sortOrderButton.getHeight() / 2
                        - (sortOrderPopupMenuDimension.height / 2)));
    }


    private JSplitPane setupJSplitPane() {
        JScrollPane listScrollPane = new JScrollPane(list);
        listScrollPane.setBorder(BorderFactory.createEmptyBorder());
        listScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        JPanel listPanel = new JPanel(new BorderLayout());
        listPanel.setOpaque(true);
        listPanel.setBackground(UIManager.getColor("Spinner.background"));
        listPanel.add(sortOrderButton, BorderLayout.NORTH);
        listPanel.add(listScrollPane);

        JSplitPane splitPane = new JSplitPane();
        splitPane.setResizeWeight(1.0d);
        splitPane.setContinuousLayout(true);
        splitPane.setDividerSize(1);
        splitPane.setLeftComponent(listPanel);
        return splitPane;
    }


    private void addListeners(FlashcardSeries flashcards) {
        frame.addWindowListener(new WindowAdapter() {

            @Override public void windowClosing(WindowEvent e) {

                closeFlashcardEditor();
            }

            @Override public void windowClosed(WindowEvent event) {

                // we have to make sure that the JFrame object will be collected...
                // (the VM terminates if all frames are disposed and finally collected)
                SwingUtilities.invokeLater(System::gc);
            }

        });

        flashcards.addListDataListener(new ListDataListener() {

            // Only GUI related functionality:

            public void contentsChanged(ListDataEvent event) {

                documentChanged = true;
                frame.getRootPane().putClientProperty("Window.documentModified", Boolean.TRUE);
            }


            public void intervalAdded(ListDataEvent event) {

                documentChanged = true;
                frame.getRootPane().putClientProperty("Window.documentModified", Boolean.TRUE);

                playButton.setEnabled(true);
            }


            public void intervalRemoved(ListDataEvent event) {

                documentChanged = true;
                frame.getRootPane().putClientProperty("Window.documentModified", Boolean.TRUE);

                if (FlashcardsEditor.this.flashcards.getSize() == 0)
                    playButton.setEnabled(false);
            }

        });
    }


    private JLabel createInfoPaneLabel(@NotNull JPanel panel, @NotNull String title) {

        JLabel dummyLabel = new JLabel("wwwwwwwwwwwwwwwwwwww");

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(UIManager.getFont("TableHeader.font"));
        Box titleBox = Box.createHorizontalBox();
        titleBox.add(titleLabel);
        titleBox.add(Box.createHorizontalGlue());

        JLabel contentLabel = new JLabel("...");
        contentLabel.setHorizontalTextPosition(SwingConstants.RIGHT);
        contentLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        contentLabel.setPreferredSize(dummyLabel.getPreferredSize());
        contentLabel.setMinimumSize(dummyLabel.getPreferredSize());
        contentLabel.setFont(UIManager.getFont("List.font"));
        Box contentBox = Box.createHorizontalBox();
        contentBox.add(Box.createHorizontalGlue());
        contentBox.add(contentLabel);

        panel.add(titleBox);
        panel.add(contentBox);
        panel.add(Box.createVerticalStrut(15));

        return contentLabel;
    }


    private void updateInfoPaneLabels(@Null List<Flashcard> selectedFlashcards) {

        // clean up everything
        if (infoPaneFlashcard != null) {
            infoPaneFlashcard.removeObserver(infoPaneUpdateObserver);
            infoPaneFlashcard = null;
        }

        // configure the info pane
        if (selectedFlashcards != null && selectedFlashcards.size() > 0) {
            if (selectedFlashcards.size() == 1) {
                infoPaneFlashcard = selectedFlashcards.get(0);
                infoPaneFlashcard.addObserver(infoPaneUpdateObserver);

                dateCreatedLabel.setText(infoPaneFlashcard.getCreated().format(infoPaneDateFormatter));
                if (infoPaneFlashcard.getNotRemembered() != null)
                    lastTimeNotRemeberedLabel.setText(infoPaneFlashcard
                            .getNotRemembered().format(infoPaneDateFormatter));
                else
                    lastTimeNotRemeberedLabel.setText("N/A");
                if (infoPaneFlashcard.getRemembered() != null)
                    lastTimeRememberedLabel.setText(infoPaneFlashcard
                            .getRemembered().format(infoPaneDateFormatter));

                else
                    lastTimeRememberedLabel.setText("N/A");

                numberOfTimesShownLabel.setText(Integer.toString(infoPaneFlashcard.getShownCount()));
                numberOfTimesRememberedInARowLabel.setText(Integer.toString(infoPaneFlashcard
                        .getRememberedInARowCount()));
            } else {
                String multipleFlashcardsSelected = "multiple flashcards selected";
                dateCreatedLabel.setText(multipleFlashcardsSelected);
                lastTimeNotRemeberedLabel.setText(multipleFlashcardsSelected);
                lastTimeRememberedLabel.setText(multipleFlashcardsSelected);
                numberOfTimesShownLabel.setText(multipleFlashcardsSelected);
                numberOfTimesRememberedInARowLabel.setText(multipleFlashcardsSelected);
            }

        } else {
            String noFlashcardsSelected = "no flashcard selected";
            dateCreatedLabel.setText(noFlashcardsSelected);
            lastTimeNotRemeberedLabel.setText(noFlashcardsSelected);
            lastTimeRememberedLabel.setText(noFlashcardsSelected);
            numberOfTimesShownLabel.setText(noFlashcardsSelected);
            numberOfTimesRememberedInARowLabel.setText(noFlashcardsSelected);
        }
    }


    // Implementation of the "logic":

    public @NotNull FlashcardSeries getSortedFlashcards() {

        return sortedFlashcards;
    }


    public @NotNull JFrame getFrame() {

        return frame;
    }


    private void openFlashcardSeries() {

        fileDialog.setMode(FileDialog.LOAD);
        fileDialog.setVisible(true);
        String filename = fileDialog.getFile();
        if (filename != null) {
            if (!filename.endsWith(Store.FILE_ENDING))
                filename += Store.FILE_ENDING;

            createFlashcardsEditor(new File(fileDialog.getDirectory(), filename));
        }
    }


    private void saveFlashcardSeries() {

        if (file == null)
            saveAsFlashcardSeries();
        else
            doSave(file);
    }


    private void saveAsFlashcardSeries() {

        fileDialog.setMode(FileDialog.SAVE);
        fileDialog.setVisible(true);
        String filename = fileDialog.getFile();
        if (filename != null) {
            if (!filename.endsWith(Store.FILE_ENDING))
                filename += Store.FILE_ENDING;

            File newFile = new File(fileDialog.getDirectory(), filename);
            if (newFile.exists()) {
                if (showConfirmDialog(
                        frame,
                        "The file with the name:\n" + filename
                                + "\nalready exists.\nDo you want to overwrite the file?",
                        "Warning",
                        YES_NO_OPTION,
                        WARNING_MESSAGE) == NO_OPTION)
                    return;
            }
            doSave(newFile);
        }

    }


    private void doSave(
            @NotNull
                    File file
    ) {

        try {

            Store.saveSeries(sortedFlashcards, file);
            this.documentChanged = false;

            // saving the file was successful, let's update the window's title
            this.file = file;
            this.frame.getRootPane().putClientProperty("Window.documentModified", false);
            Utilities.setFrameTitle(frame, file);

        } catch (IOException e) {
            showMessageDialog(frame, "Could not save flashcards", "Saving the flashcards to :\n"
                    + file.getName() + "\nfailed.", ERROR_MESSAGE);
        }
    }


    private void learn() {

        Object message = JOptionPane.showInputDialog(
                frame,
                "Please choose the learning strategy:",
                "",
                INFORMATION_MESSAGE,
                null,
                LearningStrategies.values(),
                LearningStrategies.values()[0]);
        if (message != null) {

            // Potential filters do not affect the learning strategy.
            // Currently, we make the assumption that the series used by a learning strategy does
            // not change while the learning strategy is used (i.e., its next and hasNext methods are
            // called.). This is the reason why the learning strategy operates on the raw data.
            // Otherwise, if the user has applied a filter, starts a learning strategy and
            // starts editing an item that is then filtered the learning strategy would need to be
            // updated... much too complex!
            LearningStrategy ls = ((LearningStrategies) message).create(sortedFlashcards
                    .getSourceModel());

            if (!ls.hasNext()) {
                showMessageDialog(
                        frame,
                        "There are no flashcards related to your selected learning strategy.",
                        "No Flashcards",
                        INFORMATION_MESSAGE);
            } else
                learnDialog.show(ls);
        }
    }


    /**
     * Creates a new flashcard editor.
     */
    public static void newFlashcardEditor() {

        new FlashcardsEditor(DefaultFlashcardSeries.createInitialFlashcardSeries());
    }


    /**
     * @return <code>true</code> if this editor has been closed; <code>false</code> if the user
     *         aborted closing the editor.
     */
    private boolean closeFlashcardEditor() {

        if (documentChanged
                && JOptionPane.showConfirmDialog(
                        frame,
                        "Your document contains unsaved changes, close?",
                        "Confirm",
                        JOptionPane.OK_CANCEL_OPTION) == JOptionPane.CANCEL_OPTION)
            return false;

        frame.setVisible(false);
        frame.dispose(); // required to give up all resources
        return true;
    }


    private void createFlashcard() {

        Flashcard flashcard = new Flashcard();
        Command c = flashcardEditor.edit(flashcard);
        if (c != null) {
            c.execute();

            commands.execute(sortedFlashcards.createAddCardCommand(flashcard));
        }
    }


    private void removeFlashcards() {

        int[] indices = list.getSelectedIndices();
        list.clearSelection();

        commands.execute(sortedFlashcards.createRemoveCardsCommand(indices));
    }


    private void editFlashcard() {

        editFlashcard(sortedFlashcards.getElementAt(list.getSelectedIndex()));
    }


    /**
     * Opens the flashcard editor to edit the given card.
     *
     * @param flashcard
     *           The flashcard that will be edited. This flashcard must belong to the {#link
     *           FlashcardSeries} edited by this editor.
     * @return <code>true</code> if the flashcard was edited.
     */
    public boolean editFlashcard(@NotNull Flashcard flashcard) {

        Command c = flashcardEditor.edit(flashcard);
        if (c != null) {
            commands.execute(c);
            return true;
        } else
            return false;
    }

}
