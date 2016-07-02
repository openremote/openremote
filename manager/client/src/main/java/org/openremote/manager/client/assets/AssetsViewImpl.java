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
package org.openremote.manager.client.assets;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTree;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwt.view.client.TreeViewModel;

import javax.inject.Inject;
import java.util.logging.Logger;

public class AssetsViewImpl extends Composite implements AssetsView {

    private static final Logger LOG = Logger.getLogger(AssetsViewImpl.class.getName());

    interface UI extends UiBinder<HTMLPanel, AssetsViewImpl> {
    }

    private static class CustomTreeModel implements TreeViewModel {

        private final SingleSelectionModel<String> selectionModel = new SingleSelectionModel<>();

        /**
         * Get the {@link NodeInfo} that provides the children of the specified
         * value.
         */
        public <T> NodeInfo<?> getNodeInfo(T value) {
          /*
           * Create some data in a data provider. Use the parent value as a prefix
           * for the next level.
           */
            ListDataProvider<String> dataProvider = new ListDataProvider<String>();
            for (int i = 0; i < 2; i++) {
                dataProvider.getList().add(value + "." + String.valueOf(i));
            }

            // Return a node info that pairs the data with a cell.
            DefaultNodeInfo<String> nodeInfo = new DefaultNodeInfo<>(dataProvider, new TextCell(), selectionModel, null);

            selectionModel.addSelectionChangeHandler(selectionChangeEvent -> {

                LOG.info("### SELECT: " +selectionModel.getSelectedObject());
            });

            return nodeInfo;
        }

        /**
         * Check if the specified value represents a leaf node. Leaf nodes cannot be
         * opened.
         */
        public boolean isLeaf(Object value) {
            // The maximum length of a value is ten characters.
            return value.toString().length() > 10;
        }
    }

    @UiField
    SimplePanel assetTreeContainer;

    @UiField
    SimplePanel assetsContentContainer;

    Presenter presenter;

    @Inject
    public AssetsViewImpl() {
        UI ui = GWT.create(UI.class);
        initWidget(ui.createAndBindUi(this));

        assetsContentContainer.add(new Label("CONTENT"));

        TreeViewModel treeModel = new CustomTreeModel();
        CellTree tree = new CellTree(treeModel, "Item 1");

        assetTreeContainer.add(tree);

    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }

}
