package fiji.plugin.imaging_fcs.new_imfcs.view;

import javax.swing.*;

/**
 * An abstract base class for views within the application, providing a template method pattern for UI initialization.
 * This class defines a sequence of steps to configure the window and initialize UI components like
 * combo boxes, text fields, and buttons. Subclasses are required to implement the method to add these components to the frame.
 */
public abstract class BaseView extends JFrame {

    /**
     * Constructs a BaseView with the specified title.
     *
     * @param title The title of the window.
     */
    public BaseView(String title) {
        super(title);
    }

    /**
     * Initializes the user interface by configuring the window, initializing UI components,
     * and adding them to the frame. This method orchestrates the call to several protected methods,
     * each responsible for a part of the UI setup.
     */
    protected final void initializeUI() {
        configureWindow();

        initializeComboBoxes();
        initializeTextFields();
        initializeButtons();

        addComponentsToFrame();
    }

    /**
     * Configures window properties. Sets the window to be focusable, prevents closing on operation close,
     * and makes it non-resizable.
     */
    protected void configureWindow() {
        setFocusable(true);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setResizable(false);
    }

    /**
     * Initializes combo boxes in the UI. Subclasses should override this method to add and configure combo boxes.
     */
    protected void initializeComboBoxes() {
    }

    /**
     * Initializes text fields in the UI. Subclasses should override this method to add and configure text fields.
     */
    protected void initializeTextFields() {
    }

    /**
     * Initializes buttons in the UI. Subclasses should override this method to add and configure buttons.
     */
    protected void initializeButtons() {
    }

    /**
     * Abstract method that subclasses must implement to add components to the frame.
     * This method is part of the UI initialization process and is called after the components have been initialized.
     */
    protected abstract void addComponentsToFrame();
}
