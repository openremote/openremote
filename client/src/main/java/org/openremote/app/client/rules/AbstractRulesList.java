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
package org.openremote.app.client.rules;

import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import org.openremote.app.client.assets.browser.AssetBrowser;
import org.openremote.app.client.i18n.ManagerMessages;
import org.openremote.app.client.style.FormTableStyle;
import org.openremote.app.client.widget.Form;
import org.openremote.app.client.widget.FormOutputText;
import org.openremote.app.client.widget.Headline;
import org.openremote.app.client.widget.Hyperlink;
import org.openremote.model.asset.agent.AgentStatusEvent;
import org.openremote.model.event.shared.TenantFilter;
import org.openremote.model.rules.RulesEngineInfo;
import org.openremote.model.rules.Ruleset;
import org.openremote.model.rules.RulesetStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class AbstractRulesList<P extends RulesList.Presenter<R>, R extends Ruleset>
    extends Composite
    implements RulesList<P, R> {

    @UiField
    public ManagerMessages managerMessages;

    @UiField
    public HTMLPanel sidebarContainer;

    @UiField
    public HTMLPanel mainContent;

    @UiField
    public Headline headline;

    @UiField
    public Form engineStatusForm;

    @UiField
    public FormOutputText engineStatusOutput;

    @UiField
    public FormOutputText compilationErrorCountOutput;

    @UiField
    public FormOutputText executionErrorCountOutput;

    @UiField
    public Hyperlink createLink;

    @UiField
    public Label noRulesetsLabel;

    @UiField
    public RulesetTable.Style tableStyle;

    final protected RulesetTable<R> table;

    final AssetBrowser assetBrowser;

    P presenter;

    List<R> rulesets;

    public AbstractRulesList(AssetBrowser assetBrowser, FormTableStyle formTableStyle) {
        this.assetBrowser = assetBrowser;

        initComposite();

        table = new RulesetTable<>(managerMessages, tableStyle, formTableStyle);
        table.getSelectionModel().addSelectionChangeHandler(event -> {
                R selected;
                if ((selected = table.getSelectedObject()) != null
                    && presenter != null) {
                    presenter.onRulesetSelected(selected);
                }
            }
        );
        table.setVisible(false);
        mainContent.add(table);
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
        headline.setText(managerMessages.loadingDotdotdot());
        setCreateRulesetHistoryToken("");
        noRulesetsLabel.setVisible(false);
        engineStatusForm.setVisible(false);
        table.setVisible(false);
        table.setRowData(new ArrayList<>());
        table.flush();
    }

    @Override
    public void setCreateRulesetHistoryToken(String token) {
        createLink.setTargetHistoryToken(token);
        createLink.setVisible(token != null && token.length() > 0);
    }

    @Override
    public void setRulesets(List<R> rulesets) {
        noRulesetsLabel.setVisible(rulesets.isEmpty());
        engineStatusForm.setVisible(!rulesets.isEmpty());
        this.rulesets = rulesets;
        table.setVisible(!rulesets.isEmpty());
        table.setRowData(this.rulesets);
        table.flush();
    }

    @Override
    public void onEngineStatusChanged(RulesEngineInfo engineInfo) {
        if (engineInfo == null) {
            engineStatusForm.setVisible(false);
            return;
        }

        engineStatusOutput.setText(managerMessages.engineStatus(engineInfo.getStatus()));
        compilationErrorCountOutput.setText(Integer.toString(engineInfo.getCompilationErrorCount()));
        executionErrorCountOutput.setText(Integer.toString(engineInfo.getExecutionErrorCount()));
        engineStatusForm.setVisible(true);
    }

    @Override
    public void onRulesetStatusChanged(R ruleset) {
        if (rulesets == null) {
            rulesets = new ArrayList<>();
        }

        if (ruleset.getStatus() == RulesetStatus.REMOVED) {
            if (!rulesets.removeIf(r -> Objects.equals(r.getId(), ruleset.getId()))) {
                return;
            }
        } else {
            boolean matchFound = false;

            for (Ruleset existingRuleset : rulesets) {
                if (Objects.equals(existingRuleset.getId(), ruleset.getId())) {
                    matchFound = true;
                    existingRuleset.setName(ruleset.getName());
                    existingRuleset.setEnabled(ruleset.isEnabled());
                    existingRuleset.setLang(ruleset.getLang());
                    existingRuleset.setLastModified(ruleset.getLastModified());
                    existingRuleset.setStatus(ruleset.getStatus());
                    existingRuleset.setError(ruleset.getError());
                    break;
                }
            }

            if (!matchFound) {
                rulesets.add(ruleset);
            }
        }

        setRulesets(rulesets);
    }
}
