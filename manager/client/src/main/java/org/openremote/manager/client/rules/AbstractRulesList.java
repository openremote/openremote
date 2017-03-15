/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.manager.client.rules;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import org.openremote.manager.client.assets.browser.AssetBrowser;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.style.FormTableStyle;
import org.openremote.manager.client.widget.FormButton;
import org.openremote.manager.shared.rules.RulesDefinition;

import java.util.ArrayList;
import java.util.Arrays;

public abstract class AbstractRulesList<P extends RulesList.Presenter<R>, R extends RulesDefinition>
    extends Composite
    implements RulesList<P, R> {

    @UiField
    public ManagerMessages managerMessages;

    @UiField
    public HTMLPanel sidebarContainer;

    @UiField
    public RulesDefinitionTable.Style tableStyle;

    @UiField
    public FormButton createButton;

    @UiField
    public SimplePanel tableContainer;

    final protected RulesDefinitionTable<R> table;
    final AssetBrowser assetBrowser;
    P presenter;

    public AbstractRulesList(AssetBrowser assetBrowser, FormTableStyle formTableStyle) {
        this.assetBrowser = assetBrowser;

        initComposite();

        table = new RulesDefinitionTable<>(managerMessages, tableStyle, formTableStyle);
        table.getSelectionModel().addSelectionChangeHandler(event -> {
                R selected;
                if ((selected = table.getSelectedObject()) != null
                    && presenter != null) {
                    presenter.onRulesDefinitionSelected(selected);
                }
            }
        );
        tableContainer.add(table);
    }

    abstract protected void initComposite();

    @Override
    public void setPresenter(P presenter) {
        this.presenter = presenter;
        if (presenter != null) {
            assetBrowser.asWidget().removeFromParent();
            sidebarContainer.add(assetBrowser.asWidget());
        } else {
            onPresenterReset();
        }
    }

    protected void onPresenterReset() {
        sidebarContainer.clear();
        table.setRowData(new ArrayList<>());
        table.flush();
    }

    @Override
    public void setRulesDefinitions(R[] definitions) {
        tableContainer.setVisible(definitions.length > 0);
        table.setRowData(Arrays.asList(definitions));
        table.flush();
    }

    @UiHandler("createButton")
    public void createClicked(ClickEvent e) {
        presenter.createRule();
    }
}
