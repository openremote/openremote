/*
 * Copyright 2016, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.manager.client.assets.browser;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.CellTree;
import com.google.gwt.user.client.ui.*;
import org.openremote.manager.client.assets.Asset;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.style.FormTreeStyle;
import org.openremote.manager.client.widget.FormTree;
import org.openremote.manager.client.widget.PushButton;

import javax.inject.Inject;
import java.util.logging.Logger;

public class AssetBrowserImpl extends Composite implements AssetBrowser {

    private static final Logger LOG = Logger.getLogger(AssetBrowserImpl.class.getName());

    interface UI extends UiBinder<HTMLPanel, AssetBrowserImpl> {
    }

    @UiField
    ManagerMessages managerMessages;

    @UiField
    SimplePanel assetTreeContainer;

    @UiField
    TextBox searchInput;

    @UiField
    PushButton filterButton;

/*
    @UiField
    PushButton expandButton;
*/

    final FormTreeStyle formTreeStyle;

    Presenter presenter;
    FormTree assetTree;

    @Inject
    public AssetBrowserImpl(FormTreeStyle formTreeStyle) {
        this.formTreeStyle = formTreeStyle;

        UI ui = GWT.create(UI.class);
        initWidget(ui.createAndBindUi(this));
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;

        assetTree = new FormTree(
            new AssetTreeModel(presenter),
            new Asset(Asset.ROOT_ID, Asset.ROOT_TYPE, Asset.ROOT_LABEL, Asset.ROOT_LOCATION),
            formTreeStyle,
            new CellTree.CellTreeMessages() {
                @Override
                public String showMore() {
                    return managerMessages.showMoreAssets();
                }

                @Override
                public String emptyTree() {
                    return managerMessages.emptyCompositeAsset();
                }
            }
        );

        // TODO Page size and paging is not good, do something with onhover autoscroll
        assetTree.setDefaultNodeSize(1000);

        assetTreeContainer.clear();
        assetTreeContainer.add(assetTree);
    }

    @Override
    public void setSelectedAsset(String id) {
        // TODO expand tree to item
    }

/*
    @UiHandler("expandButton")
    public void expandClicked(final ClickEvent event) {
        if (assetTree != null) {
            LOG.info("### CLICK");
            NativeEvent clickEvent = Document.get().createMouseDownEvent(0, 0, 0, 0, 0, false, false, false, false, 0);
            Element target = (Element) assetTree.getElement().getLastChild().getFirstChild().getLastChild();
            target.dispatchEvent(clickEvent);
        }
    }
*/
}
