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

import com.google.gwt.cell.client.AbstractSafeHtmlCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTree;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.view.client.*;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.style.FormTreeStyle;
import org.openremote.manager.client.widget.FormTree;
import org.openremote.manager.client.widget.PushButton;

import javax.inject.Inject;
import java.util.logging.Logger;

public class AssetsViewImpl extends Composite implements AssetsView {

    private static final Logger LOG = Logger.getLogger(AssetsViewImpl.class.getName());

    interface UI extends UiBinder<HTMLPanel, AssetsViewImpl> {
    }

    static class AssetCell extends AbstractSafeHtmlCell<Asset> {

        static final SafeHtmlRenderer<Asset> assetRenderer = new SafeHtmlRenderer<Asset>() {
            public SafeHtml render(Asset object) {
                return (object == null)
                    ? SafeHtmlUtils.EMPTY_SAFE_HTML
                    : SafeHtmlUtils.fromString(object.getDisplayName()
                );
            }

            public void render(Asset object, SafeHtmlBuilder appendable) {
                appendable.append(SafeHtmlUtils.fromString(object.getDisplayName()));
            }
        };

        public AssetCell() {
            super(assetRenderer);
        }

        @Override
        public void render(Cell.Context context, SafeHtml value, SafeHtmlBuilder sb) {
            if (value != null) {
                sb.append(value);
            }
        }
    }

    static class AssetTreeModel implements TreeViewModel {

        final Presenter presenter;
        final SingleSelectionModel<Asset> selectionModel = new SingleSelectionModel<>();

        public AssetTreeModel(Presenter presenter) {
            this.presenter = presenter;
            selectionModel.addSelectionChangeHandler(selectionChangeEvent -> {
                presenter.onAssetSelected(selectionModel.getSelectedObject());
            });
        }

        public <T> NodeInfo<?> getNodeInfo(T value) {
            return new DefaultNodeInfo<>(
                new AssetDataProvider(presenter, (Asset) value),
                new AssetCell(),
                selectionModel,
                null);
        }

        public boolean isLeaf(Object value) {
            if (value instanceof Asset) {
                Asset asset = (Asset) value;
                return !asset.getType().equals("Composite");
            }
            return true;
        }
    }

    static class AssetDataProvider extends AsyncDataProvider<Asset> {

        final protected Presenter presenter;
        final protected Asset parent;

        public AssetDataProvider(Presenter presenter, Asset parent) {
            this.presenter = presenter;
            this.parent = parent;
        }

        @Override
        protected void onRangeChanged(HasData<Asset> display) {
            final Range range = display.getVisibleRange();
            presenter.loadAssetChildren(parent, assetList -> {
                int start = range.getStart();
                updateRowCount(assetList.size(), true);
                updateRowData(start, assetList);
            });
        }
    }


    @UiField
    ManagerMessages managerMessages;

    @UiField
    SimplePanel assetTreeContainer;

    @UiField
    SimplePanel assetsContentContainer;
    @UiField
    TextBox searchInput;
    @UiField
    PushButton searchButton;

    final FormTreeStyle formTreeStyle;

    Presenter presenter;

    @Inject
    public AssetsViewImpl(FormTreeStyle formTreeStyle) {
        this.formTreeStyle = formTreeStyle;

        UI ui = GWT.create(UI.class);
        initWidget(ui.createAndBindUi(this));

        assetsContentContainer.add(new Label("TODO: Asset Editors"));
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
        FormTree tree = new FormTree(
            new AssetTreeModel(presenter),
            new Asset(null, "Composite", "ROOT", null),
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
        assetTreeContainer.clear();
        assetTreeContainer.add(tree);
    }

    @Override
    public void setAssetDisplayName(String name) {
        assetsContentContainer.clear();
        assetsContentContainer.add(new Label("TODO: Selected " + name));
    }
}
