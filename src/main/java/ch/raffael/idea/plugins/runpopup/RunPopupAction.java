package ch.raffael.idea.plugins.runpopup;

import javax.swing.JComponent;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.ui.LayeredIcon;
import com.intellij.ui.awt.RelativePoint;


/**
 * The main Run popup action.
 *
 * @author Raffael Herzog
 */
public class RunPopupAction extends AnAction implements CustomComponentAction{

    public RunPopupAction() {
        super("Run Popup", "Show a run popup menu",
                new LayeredIcon(AllIcons.Actions.Execute, AllIcons.General.Dropdown));
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        DataContext dataContext = e.getDataContext();
        Project project = dataContext.getData(CommonDataKeys.PROJECT);
        if ( project == null ) {
            return;
        }
        JComponent actionButton = null;
        RunPopupActionGroup group = new RunPopupActionGroup();
        if ( ActionPlaces.isToolbarPlace(e.getPlace()) ) {
            actionButton = (JComponent)e.getPresentation().getClientProperty(CUSTOM_COMPONENT_PROPERTY);
        }
        JBPopupFactory popupFactory = JBPopupFactory.getInstance();
        ListPopup popup = popupFactory.createActionGroupPopup(
                actionButton == null ? "Run" : null,
                group,
                dataContext, JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, false);
        //popup.addListener(new JBPopupAdapter() {
        //    @Override
        //    public void beforeShown(LightweightWindowEvent event) {
        //        JComponent component = event.asPopup().getContent();
        //        component.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("TAB"), "favorites-others");
        //        component.getActionMap().put("favorites-others", new AbstractAction() {
        //            @Override
        //            public void actionPerformed(ActionEvent e) {
        //                ListPopupStep step = popup.getListStep();
        //                if ( group.getFirstNonFavoriteIndex() != null ) {
        //                }
        //                System.out.println("TAB");
        //            }
        //        });
        //    }
        //});
        if ( actionButton == null ) {
            popup.showCenteredInCurrentWindow(project);
        }
        else {
            popup.show(RelativePoint.getSouthWestOf(actionButton));
        }
    }

    @Override
    public JComponent createCustomComponent(Presentation presentation) {
        return new ActionButton(this, presentation, ActionPlaces.TOOLBAR, ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE);
    }
    //@Override
    //public JComponent createCustomComponent(Presentation presentation) {
    //    return new JLabel("blubber di blubb");
    //}
}
