package org.openremote.manager.client.assets.browser;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.widget.*;
import org.openremote.model.Consumer;

/**
 * Shows an browser tree node in a form group, and encapsulates the process of
 * changing it by selecting a new node in the asset browser tree.
 */
public abstract class AssetSelector extends FormGroup {

    final AssetBrowser.Presenter assetBrowserPresenter;
    final ManagerMessages managerMessages;
    final Consumer<BrowserTreeNode> changeConsumer;

    final FormLabel label = new FormLabel();
    final FormField field = new FormField();
    final FormGroupActions actions = new FormGroupActions();
    final FlowPanel fieldContainer = new FlowPanel();
    final FormOutputText outputTenantDisplayName = new FormOutputText();
    final FormInputText outputAssetName = new FormInputText();
    final Label infoLabel = new Label();
    final FormButton selectAssetButton = new FormButton();
    final FormButton confirmButton = new FormButton();

    // The node that was selected before the selection process started
    protected BrowserTreeNode originalNode;

    // The node that is currently selected
    protected BrowserTreeNode selectedNode;

    public AssetSelector(AssetBrowser.Presenter assetBrowserPresenter,
                         ManagerMessages managerMessages,
                         String labelText,
                         String infoText,
                         Consumer<BrowserTreeNode> changeConsumer) {
        this.assetBrowserPresenter = assetBrowserPresenter;
        this.managerMessages = managerMessages;
        this.changeConsumer = changeConsumer;
        addFormLabel(label);
        addFormField(field);
        addFormGroupActions(actions);
        addInfolabel(infoLabel);
        field.add(fieldContainer);
        fieldContainer.setStyleName("layout vertical");
        fieldContainer.add(outputTenantDisplayName);
        fieldContainer.add(outputAssetName);
        actions.add(selectAssetButton);
        actions.add(confirmButton);

        setAlignStart(true);

        formLabel.setText(labelText);
        infoLabel.setText(infoText);

        outputAssetName.setReadOnly(true);
        outputAssetName.addStyleName("flex");

        selectAssetButton.setIcon("external-link-square");
        selectAssetButton.setText(managerMessages.selectAsset());
        selectAssetButton.addClickHandler(event -> beginSelection());

        confirmButton.setIcon("check");
        confirmButton.setText(managerMessages.OK());
        confirmButton.addClickHandler(event -> endSelection());

        init();
    }

    public void init() {
        originalNode = null;
        selectedNode = null;
        outputTenantDisplayName.setText(null);
        outputAssetName.setText(null);
        infoLabel.setVisible(false);
        selectAssetButton.setVisible(true);
        confirmButton.setVisible(false);
        assetBrowserPresenter.useSelector(null);
    }

    public void beginSelection() {
        selectAssetButton.setVisible(false);
        confirmButton.setVisible(true);
        infoLabel.setVisible(true);

        assetBrowserPresenter.useSelector(this);
        originalNode = assetBrowserPresenter.getSelectedNode();
        assetBrowserPresenter.clearSelection();
    }

    public void setSelectedNode(BrowserTreeNode selectedNode) {
        this.selectedNode = selectedNode;
        if (selectedNode instanceof TenantTreeNode) {
            outputTenantDisplayName.setText(selectedNode.getLabel());
            outputAssetName.setText(managerMessages.assetHasNoParent());
        } else if (selectedNode instanceof AssetTreeNode) {
            AssetTreeNode assetTreeNode = (AssetTreeNode) selectedNode;
            outputTenantDisplayName.setText(assetTreeNode.getTenantDisplayName());
            outputAssetName.setText(selectedNode.getLabel());
        }
    }

    public void endSelection() {
        assetBrowserPresenter.useSelector(null);
        if (originalNode != null) {
            if (originalNode instanceof TenantTreeNode) {
                assetBrowserPresenter.selectTenant(originalNode.getId());
            } else if (originalNode instanceof AssetTreeNode) {
                assetBrowserPresenter.selectAssetById(originalNode.getId());
            }
        } else {
            assetBrowserPresenter.clearSelection();
        }
        selectAssetButton.setVisible(true);
        confirmButton.setVisible(false);
        infoLabel.setVisible(false);
        if (originalNode == null || !originalNode.getId().equals(selectedNode.getId())) {
            changeConsumer.accept(selectedNode);
        }
    }

    public void setEnabled(boolean enabled) {
        selectAssetButton.setEnabled(enabled);
    }
}
