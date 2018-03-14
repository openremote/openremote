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

import com.google.gwt.cell.client.Cell;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.view.client.SingleSelectionModel;
import org.openremote.app.client.i18n.ManagerMessages;
import org.openremote.app.client.style.FormTableStyle;
import org.openremote.app.client.widget.FormTable;
import org.openremote.app.client.widget.IconCell;
import org.openremote.model.rules.Ruleset;
import org.openremote.model.Constants;

public class RulesetTable<R extends Ruleset> extends FormTable<R> {

    static protected DateTimeFormat dateTimeFormat = DateTimeFormat.getFormat(Constants.DEFAULT_DATETIME_FORMAT);

    public interface Style extends CssResource {
        String nameColumn();

        String langColumn();

        String enabledColumn();

        String templateColumn();

        String createOnColumn();

        String lastModifiedColumn();
    }

    final protected Style style;

    final protected SingleSelectionModel<R> selectionModel = new SingleSelectionModel<>();

    final protected TextColumn<R> nameColumn = new TextColumn<R>() {
        @Override
        public String getValue(R ruleset) {
            return ruleset.getName();
        }
    };

    final protected TextColumn<R> createOnColumn = new TextColumn<R>() {
        @Override
        public String getValue(R ruleset) {
            return ruleset.getCreatedOn() != null ? dateTimeFormat.format(ruleset.getCreatedOn()) : "-";
        }

        @Override
        public String getCellStyleNames(Cell.Context context, R object) {
            return "nowrap";
        }
    };

    final protected TextColumn<R> lastModifiedColumn = new TextColumn<R>() {
        @Override
        public String getValue(R ruleset) {
            return ruleset.getLastModified() != null ? dateTimeFormat.format(ruleset.getLastModified()) : "-";
        }

        @Override
        public String getCellStyleNames(Cell.Context context, R object) {
            return "nowrap";
        }
    };

    final protected TextColumn<R> langColumn = new TextColumn<R>() {
        @Override
        public String getValue(R ruleset) {
            return ruleset.getLang().toString();
        }

        @Override
        public String getCellStyleNames(Cell.Context context, R object) {
            return "nowrap";
        }
    };

    final protected Column<R, String> enabledColumn = new Column<R, String>(new IconCell()) {
        @Override
        public String getValue(R ruleset) {
            return ruleset.isEnabled() ? "check-circle" : "circle-thin";
        }
    };

    final protected Column<R, String> templateColumn = new Column<R, String>(new IconCell()) {
        @Override
        public String getValue(R ruleset) {
            return ruleset.getTemplateAssetId() != null ? "check-circle" : "circle-thin";
        }
    };

    public RulesetTable(ManagerMessages managerMessages,
                        Style style,
                        FormTableStyle formTableStyle) {
        super(Integer.MAX_VALUE, formTableStyle);

        this.style = style;

        setSelectionModel(selectionModel);

        int i = 0;

        applyStyleCellText(nameColumn);
        addColumn(nameColumn, createHeader(managerMessages.rulesetName()));
        addColumnStyleName(i++, style.nameColumn());

        applyStyleCellText(createOnColumn);
        addColumn(createOnColumn, createHeader(managerMessages.createdOn()));
        addColumnStyleName(i++, style.createOnColumn());
        createOnColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);

        applyStyleCellText(lastModifiedColumn);
        addColumn(lastModifiedColumn, createHeader(managerMessages.lastModifiedOn()));
        addColumnStyleName(i++, style.lastModifiedColumn());
        lastModifiedColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);

        addColumn(langColumn, createHeader(managerMessages.language()));
        addColumnStyleName(i++, style.langColumn());
        langColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);

        addColumn(enabledColumn, createHeader(managerMessages.enabled()));
        addColumnStyleName(i++, style.enabledColumn());
        enabledColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);

        addColumn(templateColumn, createHeader(managerMessages.template()));
        addColumnStyleName(i++, style.templateColumn());
        templateColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
    }

    @Override
    public SingleSelectionModel<R> getSelectionModel() {
        return selectionModel;
    }

    public R getSelectedObject() {
        return getSelectionModel().getSelectedObject();
    }
}
