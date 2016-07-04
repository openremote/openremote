package org.openremote.manager.client.style;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.cellview.client.CellTree;

import javax.inject.Inject;

public class FormTreeStyle implements CellTree.Style {

    final protected StyleClientBundle styleClientBundle;
    final protected WidgetStyle widgetStyle;
    final protected ThemeStyle themeStyle;

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
    public FormTreeStyle(StyleClientBundle styleClientBundle, WidgetStyle widgetStyle, ThemeStyle themeStyle) {
        this.styleClientBundle = styleClientBundle;
        this.widgetStyle = widgetStyle;
        this.themeStyle = themeStyle;
    }

    public CellTree.Resources getCellTreeResources() {
        return cellTreeResources;
    }

    public WidgetStyle getWidgetStyle() {
        return widgetStyle;
    }

    public ThemeStyle getThemeStyle() {
        return themeStyle;
    }

    @Override
    public String cellTreeEmptyMessage() {
        return widgetStyle.FormTreeEmptyMessage() + " " + themeStyle.FormTreeEmptyMessage();
    }

    @Override
    public String cellTreeItem() {
        return widgetStyle.FormTreeItem() + " " + themeStyle.FormTreeItem();
    }

    @Override
    public String cellTreeItemImage() {
        return widgetStyle.FormTreeItemImage() + " " + themeStyle.FormTreeItemImage();
    }

    @Override
    public String cellTreeItemImageValue() {
        return widgetStyle.FormTreeItemImageValue() + " " + themeStyle.FormTreeItemImageValue();
    }

    @Override
    public String cellTreeItemValue() {
        return widgetStyle.FormTreeItemValue() + " " + themeStyle.FormTreeItemValue();
    }

    @Override
    public String cellTreeKeyboardSelectedItem() {
        return widgetStyle.FormTreeKeyboardSelectedItem() + " " + themeStyle.FormTreeKeyboardSelectedItem();
    }

    @Override
    public String cellTreeOpenItem() {
        return widgetStyle.FormTreeOpenItem() + " " + themeStyle.FormTreeOpenItem();
    }

    @Override
    public String cellTreeSelectedItem() {
        return widgetStyle.FormTreeSelectedItem() + " " + themeStyle.FormTreeSelectedItem();
    }

    @Override
    public String cellTreeShowMoreButton() {
        return widgetStyle.FormTreeShowMoreButton() + " " + themeStyle.FormTreeShowMoreButton();
    }

    @Override
    public String cellTreeTopItem() {
        return widgetStyle.FormTreeTopItem() + " " + themeStyle.FormTreeTopItem();
    }

    @Override
    public String cellTreeTopItemImage() {
        return widgetStyle.FormTreeTopItemImage() + " " + themeStyle.FormTreeTopItemImage();
    }

    @Override
    public String cellTreeTopItemImageValue() {
        return widgetStyle.FormTreeTopItemImageValue() + " " + themeStyle.FormTreeTopItemImageValue();
    }

    @Override
    public String cellTreeWidget() {
        return widgetStyle.FormTreeWidget() + " " + themeStyle.FormTreeWidget();
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