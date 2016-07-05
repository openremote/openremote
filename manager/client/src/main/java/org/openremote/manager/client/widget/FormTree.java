package org.openremote.manager.client.widget;

import com.google.gwt.user.cellview.client.CellTree;
import com.google.gwt.view.client.TreeViewModel;
import org.openremote.manager.client.style.FormTreeStyle;
import org.openremote.manager.client.style.ThemeStyle;
import org.openremote.manager.client.style.WidgetStyle;

import java.util.logging.Logger;

public class FormTree extends CellTree {

    private static final Logger LOG = Logger.getLogger(FormTree.class.getName());


    final protected WidgetStyle widgetStyle;
    final protected ThemeStyle themeStyle;

    public <T> FormTree(TreeViewModel viewModel, T rootValue, FormTreeStyle formTreeStyle, CellTreeMessages messages) {
        super(viewModel, rootValue, formTreeStyle.getCellTreeResources(), messages);
        this.widgetStyle = formTreeStyle.getWidgetStyle();
        this.themeStyle = formTreeStyle.getThemeStyle();
    }


}
