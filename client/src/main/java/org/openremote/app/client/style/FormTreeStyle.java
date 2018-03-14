package org.openremote.app.client.style;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.cellview.client.CellTree;
import org.openremote.app.client.style.WidgetStyle;

import javax.inject.Inject;

public class FormTreeStyle implements CellTree.Style {

    final protected StyleClientBundle styleClientBundle;
    final protected WidgetStyle widgetStyle;

    final protected CellTree.Resources cellTreeResources = new CellTree.Resources() {

        @Override
        public ImageResource cellTreeClosedItem() {
            return styleClientBundle.FormTreeClosedItem();
        }

        @Override
        public ImageResource cellTreeLoading() {
            return styleClientBundle.FormTreeLoading();
        }

        @Override
        public ImageResource cellTreeOpenItem() {
            return styleClientBundle.FormTreeOpenItem();
        }

        @Override
        public ImageResource cellTreeSelectedBackground() {
            return null;
        }

        @Override
        public CellTree.Style cellTreeStyle() {
            return FormTreeStyle.this;
        }
    };

    @Inject
    public FormTreeStyle(StyleClientBundle styleClientBundle, WidgetStyle widgetStyle) {
        this.styleClientBundle = styleClientBundle;
        this.widgetStyle = widgetStyle;
    }

    public CellTree.Resources getCellTreeResources() {
        return cellTreeResources;
    }

    public WidgetStyle getWidgetStyle() {
        return widgetStyle;
    }

    @Override
    public String cellTreeEmptyMessage() {
        return widgetStyle.FormTreeEmptyMessage();
    }

    @Override
    public String cellTreeItem() {
        return widgetStyle.FormTreeItem();
    }

    @Override
    public String cellTreeItemImage() {
        return widgetStyle.FormTreeItemImage();
    }

    @Override
    public String cellTreeItemImageValue() {
        return widgetStyle.FormTreeItemImageValue();
    }

    @Override
    public String cellTreeItemValue() {
        return widgetStyle.FormTreeItemValue();
    }

    @Override
    public String cellTreeKeyboardSelectedItem() {
        return widgetStyle.FormTreeKeyboardSelectedItem();
    }

    @Override
    public String cellTreeOpenItem() {
        return widgetStyle.FormTreeOpenItem();
    }

    @Override
    public String cellTreeSelectedItem() {
        return widgetStyle.FormTreeSelectedItem();
    }

    @Override
    public String cellTreeShowMoreButton() {
        return widgetStyle.FormTreeShowMoreButton();
    }

    @Override
    public String cellTreeTopItem() {
        return widgetStyle.FormTreeTopItem();
    }

    @Override
    public String cellTreeTopItemImage() {
        return widgetStyle.FormTreeTopItemImage();
    }

    @Override
    public String cellTreeTopItemImageValue() {
        return widgetStyle.FormTreeTopItemImageValue();
    }

    @Override
    public String cellTreeWidget() {
        return widgetStyle.FormTreeWidget();
    }

    @Override
    public boolean ensureInjected() {
        return false;
    }

    @Override
    public String getText() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }
}