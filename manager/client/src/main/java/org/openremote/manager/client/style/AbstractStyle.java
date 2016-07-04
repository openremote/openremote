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
package org.openremote.manager.client.style;

public abstract class AbstractStyle {

    abstract String getPrefix();

    public String Viewport() {
        return getPrefix() + "Viewport";
    }

    public String Header() {
        return getPrefix() + "Header";
    }

    public String Footer() {
        return getPrefix() + "Footer";
    }

    public String NavItem() {
        return getPrefix() + "NavItem";
    }

    public String SecondaryNav() {
        return getPrefix() + "SecondaryNav";
    }

    public String SecondaryNavItem() {
        return getPrefix() + "SecondaryNavItem";
    }

    public String SidebarContent() {
        return getPrefix() + "SidebarContent";
    }

    public String MainContent() {
        return getPrefix() + "MainContent";
    }

    public String PushButton() {
        return getPrefix() + "PushButton";
    }

    public String Hyperlink() {
        return getPrefix() + "Hyperlink";
    }

    public String UnorderedList() {
        return getPrefix() + "UnorderedList";
    }

    public String Toast() {
        return getPrefix() + "Toast";
    }

    public String ToastInfo() {
        return getPrefix() + "ToastInfo";
    }

    public String ToastFailure() {
        return getPrefix() + "ToastFailure";
    }

    public String MessagesIcon() {
        return getPrefix() + "MessagesIcon";
    }

    public String PopupPanel() {
        return getPrefix() + "PopupPanel";
    }

    public String PopupPanelHeader() {
        return getPrefix() + "PopupPanelHeader";
    }

    public String PopupPanelContent() {
        return getPrefix() + "PopupPanelContent";
    }

    public String PopupPanelFooter() {
        return getPrefix() + "PopupPanelFooter";
    }

    public String Headline1() {
        return getPrefix() + "Headline1";
    }

    public String HeadlineSub() {
        return getPrefix() + "HeadlineSub";
    }

    public String Form() {
        return getPrefix() + "Form";
    }

    public String FormMessages() {
        return getPrefix() + "FormMessages";
    }

    public String FormBusy() {
        return getPrefix() + "FormBusy";
    }

    public String FormGroup() {
        return getPrefix() + "FormGroup";
    }

    public String FormLabel() {
        return getPrefix() + "FormLabel";
    }

    public String FormField() {
        return getPrefix() + "FormField";
    }

    public String FormControl() {
        return getPrefix() + "FormControl";
    }

    public String FormInputText() {
        return getPrefix() + "FormInputText";
    }

    public String FormDropDown() {
        return getPrefix() + "FormDropDown";
    }

    public String FormButton() {
        return getPrefix() + "FormButton";
    }

    public String FormButtonPrimary() {
        return getPrefix() + "FormButtonPrimary";
    }

    public String FormButtonDanger() {
        return getPrefix() + "FormButtonDanger";
    }

    public String FormCheckBox() {
        return getPrefix() + "FormCheckBox";
    }

    public String FormTableHeaderCell() {
        return getPrefix() + "FormTableHeaderCell";
    }

    public String FormTableCell() {
        return getPrefix() + "FormTableCell";
    }

    public String FormTableCellText() {
        return getPrefix() + "FormTableCellText";
    }

    public String FormTableEvenRow() {
        return getPrefix() + "FormTableEvenRow";
    }

    public String FormTableEvenRowCell() {
        return getPrefix() + "FormTableEvenRowCell";
    }

    public String FormTableFirstColumn() {
        return getPrefix() + "FormTableFirstColumn";
    }

    public String FormTableFirstColumnFooter() {
        return getPrefix() + "FormTableFirstColumnFooter";
    }

    public String FormTableFirstColumnHeader() {
        return getPrefix() + "FormTableFirstColumnHeader";
    }

    public String FormTableFooter() {
        return getPrefix() + "FormTableFooter";
    }

    public String FormTableHeader() {
        return getPrefix() + "FormTableHeader";
    }

    public String FormTableHoveredRow() {
        return getPrefix() + "FormTableHoveredRow";
    }

    public String FormTableHoveredRowCell() {
        return getPrefix() + "FormTableHoveredRowCell";
    }

    public String FormTableKeyboardSelectedCell() {
        return getPrefix() + "FormTableKeyboardSelectedCell";
    }

    public String FormTableKeyboardSelectedRow() {
        return getPrefix() + "FormTableKeyboardSelectedRow";
    }

    public String FormTableKeyboardSelectedRowCell() {
        return getPrefix() + "FormTableKeyboardSelectedRowCell";
    }

    public String FormTableLastColumn() {
        return getPrefix() + "FormTableLastColumn";
    }

    public String FormTableLastColumnHeader() {
        return getPrefix() + "FormTableLastColumnHeader";
    }

    public String FormTableLastColumnFooter() {
        return getPrefix() + "FormTableLastColumnFooter";
    }

    public String FormTableLoading() {
        return getPrefix() + "FormTableLoading";
    }

    public String FormTableOddRow() {
        return getPrefix() + "FormTableOddRow";
    }

    public String FormTableOddRowCell() {
        return getPrefix() + "FormTableOddRowCell";
    }

    public String FormTableSelectedRow() {
        return getPrefix() + "FormTableSelectedRow";
    }

    public String FormTableSelectedRowCell() {
        return getPrefix() + "FormTableSelectedRowCell";
    }

    public String FormTableSortableHeader() {
        return getPrefix() + "FormTableSortableHeader";
    }

    public String FormTableSortedHeaderAscending() {
        return getPrefix() + "FormTableSortedHeaderAscending";
    }

    public String FormTableSortedHeaderDescending() {
        return getPrefix() + "FormTableSortedHeaderDescending";
    }

    public String FormTableWidget() {
        return getPrefix() + "FormTableWidget";
    }

    public String FormTreeEmptyMessage() {
        return getPrefix() + "FormTreeEmptyMessage";
    }

    public String FormTreeItem(){
        return getPrefix() + "FormTreeItem";
    }

    public String FormTreeItemImage(){
        return getPrefix() + "FormTreeItemImage";
    }

    public String FormTreeItemImageValue(){
        return getPrefix() + "FormTreeItemImageValue";
    }

    public String FormTreeItemValue(){
        return getPrefix() + "FormTreeItemValue";
    }

    public String FormTreeKeyboardSelectedItem(){
        return getPrefix() + "FormTreeKeyboardSelectedItem";
    }

    public String FormTreeSelectedItem(){
        return getPrefix() + "FormTreeSelectedItem";
    }

    public String FormTreeOpenItem(){
        return getPrefix() + "FormTreeOpenItem";
    }

    public String FormTreeShowMoreButton(){
        return getPrefix() + "FormTreeShowMoreButton";
    }

    public String FormTreeTopItem(){
        return getPrefix() + "FormTreeTopItem";
    }

    public String FormTreeTopItemImage(){
        return getPrefix() + "FormTreeTopItemImage";
    }

    public String FormTreeTopItemImageValue(){
        return getPrefix() + "FormTreeTopItemImageValue";
    }

    public String FormTreeWidget(){
        return getPrefix() + "FormTreeWidget";
    }

}
