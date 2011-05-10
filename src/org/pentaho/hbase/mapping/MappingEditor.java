/* Copyright (c) 2011 Pentaho Corporation.  All rights reserved. 
 * This software was developed by Pentaho Corporation and is provided under the terms 
 * of the GNU Lesser General Public License, Version 2.1. You may not use 
 * this file except in compliance with the license. If you need a copy of the license, 
 * please go to http://www.gnu.org/licenses/lgpl-2.1.txt. The Original Code is Pentaho 
 * Data Integration.  The Initial Developer is Pentaho Corporation.
 *
 * Software distributed under the GNU Lesser Public License is distributed on an "AS IS" 
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or  implied. Please refer to 
 * the license for the specific language governing your rights and limitations.
 */

package org.pentaho.hbase.mapping;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.util.Bytes;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.steps.hbaseinput.Messages;
import org.pentaho.di.ui.core.PropsUI;
import org.pentaho.di.ui.core.dialog.ErrorDialog;
import org.pentaho.di.ui.core.gui.GUIResource;
import org.pentaho.di.ui.core.widget.ColumnInfo;
import org.pentaho.di.ui.core.widget.ComboValuesSelectionListener;
import org.pentaho.di.ui.core.widget.TableView;

public class MappingEditor extends Composite {
  
  protected Shell m_shell;
  protected Composite m_parent;
  
  protected boolean m_allowTableCreate;
  
  // table name line
  protected CCombo m_existingTableNamesCombo;
  protected boolean m_familiesInvalidated;
  
  // mapping name line
  protected CCombo m_existingMappingNamesCombo;
  
  // fields view
  protected TableView m_fieldsView;
  protected ColumnInfo m_keyCI;
  protected ColumnInfo m_familyCI;
  protected ColumnInfo m_typeCI;
  
  protected Button m_saveBut;
  protected Button m_deleteBut;
  
  protected Button m_getFieldsBut;
  
  protected MappingAdmin m_admin;
  
  protected ConfigurationProducer m_configProducer;
  protected FieldProducer m_incomingFieldsProducer;
  
  /** default family name to use when creating a new table using incoming fields */
  protected static final String DEFAULT_FAMILY = "Family1";
  
  public MappingEditor(Shell shell, Composite parent, ConfigurationProducer configProducer, 
      FieldProducer fieldProducer, int tableViewStyle, boolean allowTableCreate, 
      PropsUI props, TransMeta transMeta) {
//    super(parent, SWT.NO_BACKGROUND | SWT.NO_FOCUS | SWT.NO_MERGE_PAINTS);
    super(parent, SWT.NONE);
    
    m_shell = shell;
    m_parent = parent;
    m_configProducer = configProducer;
    m_incomingFieldsProducer = fieldProducer;
    
    m_allowTableCreate = allowTableCreate;
    int middle = props.getMiddlePct();
    int margin = Const.MARGIN;
    
    FormLayout controlLayout = new FormLayout();
    /*controlLayout.marginLeft = 0;
    controlLayout.marginRight = 0;
    controlLayout.marginTop = 0;
    controlLayout.marginBottom = 0; */
    controlLayout.marginWidth = 3;
    controlLayout.marginHeight = 3;
    
    setLayout(controlLayout);
    props.setLook(this);
    
    // table names
    Label tableNameLab = new Label(this, SWT.RIGHT);
    tableNameLab.setText(Messages.getString("MappingDialog.TableName.Label"));
    props.setLook(tableNameLab);
    FormData fd = new FormData();
    fd.left = new FormAttachment(0, 0);
    fd.top = new FormAttachment(0, margin);
    fd.right = new FormAttachment(middle, -margin);
    tableNameLab.setLayoutData(fd);
    
/*    m_existingTableNamesBut = new Button(this, SWT.PUSH | SWT.CENTER);
    props.setLook(m_existingTableNamesBut);
    m_existingTableNamesBut.setText("Get existing table names");
    fd = new FormData();
    fd.right = new FormAttachment(100, 0);
    fd.top = new FormAttachment(0, 0);
    m_existingTableNamesBut.setLayoutData(fd); */
    
    m_existingTableNamesCombo = new CCombo(this, SWT.BORDER);
    props.setLook(m_existingTableNamesCombo);
    fd = new FormData();
    fd.left = new FormAttachment(middle, 0);
    fd.top = new FormAttachment(0, margin);
    fd.right = new FormAttachment(100, 0);
    m_existingTableNamesCombo.setLayoutData(fd);
    
    // allow or disallow table creation by enabling/disabling the ability
    // to type into this combo
    m_existingTableNamesCombo.setEditable(m_allowTableCreate);
    
    // mapping names
    Label mappingNameLab = new Label(this, SWT.RIGHT);
    mappingNameLab.setText(Messages.getString("MappingDialog.MappingName.Label"));
    props.setLook(tableNameLab);
    fd = new FormData();
    fd.left = new FormAttachment(0, 0);
    fd.top = new FormAttachment(m_existingTableNamesCombo, margin);
    fd.right = new FormAttachment(middle, -margin);
    mappingNameLab.setLayoutData(fd);
    
    /*m_existingMappingNamesBut = new Button(this, SWT.PUSH | SWT.CENTER);
    props.setLook(m_existingMappingNamesBut);
    m_existingMappingNamesBut.setText("Get mapping names");
    fd = new FormData();
    fd.right = new FormAttachment(100, 0);
    fd.top = new FormAttachment(m_existingTableNamesCombo, 0);
    m_existingMappingNamesBut.setLayoutData(fd); */
    
    m_existingMappingNamesCombo = new CCombo(this, SWT.BORDER);
    props.setLook(m_existingMappingNamesCombo);
    fd = new FormData();
    fd.left = new FormAttachment(middle, 0);
    fd.top = new FormAttachment(m_existingTableNamesCombo, margin);
    //fd.right = new FormAttachment(m_existingMappingNamesBut, -margin);
    fd.right = new FormAttachment(100, 0);
    m_existingMappingNamesCombo.setLayoutData(fd);
    
    m_existingTableNamesCombo.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        m_familiesInvalidated = true;
        populateMappingComboAndFamilyStuff();
      }
      
      public void widgetDefaultSelected(SelectionEvent e) {
        m_familiesInvalidated = true;
        populateMappingComboAndFamilyStuff();
      }
    });
    
    m_existingTableNamesCombo.addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent e) {
        m_familiesInvalidated = true;
      }
    });
    
    m_existingTableNamesCombo.addFocusListener(new FocusListener() {      
      public void focusGained(FocusEvent e) {
        populateTableCombo(false);
      }
      
      public void focusLost(FocusEvent e) {
        m_familiesInvalidated = true;
        populateMappingComboAndFamilyStuff();
      }
    });
    
    m_existingMappingNamesCombo.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        loadTableViewFromMapping();
      }
      
      public void widgetDefaultSelected(SelectionEvent e) {
        loadTableViewFromMapping();
      }
    });
    
    // fields
    ColumnInfo[] colinf=new ColumnInfo[] {
        new ColumnInfo(Messages.getString("HBaseInputDialog.Fields.FIELD_ALIAS"), ColumnInfo.COLUMN_TYPE_TEXT, false),
        new ColumnInfo(Messages.getString("HBaseInputDialog.Fields.FIELD_KEY"), ColumnInfo.COLUMN_TYPE_CCOMBO, true),
        new ColumnInfo(Messages.getString("HBaseInputDialog.Fields.FIELD_FAMILY"), ColumnInfo.COLUMN_TYPE_CCOMBO, true),
        new ColumnInfo(Messages.getString("HBaseInputDialog.Fields.FIELD_NAME"), ColumnInfo.COLUMN_TYPE_TEXT, false),
        new ColumnInfo(Messages.getString("HBaseInputDialog.Fields.FIELD_TYPE"), ColumnInfo.COLUMN_TYPE_CCOMBO, true),         
        new ColumnInfo(Messages.getString("HBaseInputDialog.Fields.FIELD_INDEXED"), ColumnInfo.COLUMN_TYPE_TEXT, false),};
    
    m_keyCI = colinf[1];
    m_keyCI.setComboValues(new String[] {"N", "Y"});
    m_familyCI = colinf[2];
    m_familyCI.setComboValues(new String[] {""});
    m_typeCI = colinf[4];
    // default types for non-key fields
    m_typeCI.setComboValues(new String[] {"String", "Integer", "Long", "Float", 
        "Double", "Date", "BigNumber", "Serializable", "Binary"});
    
    m_keyCI.setComboValuesSelectionListener(new ComboValuesSelectionListener() {
      public String[] getComboValues(TableItem tableItem, int rowNr, int colNr) {
        
        tableItem.setText(5, "");
        return m_keyCI.getComboValues();
      }
    });
    
    m_typeCI.setComboValuesSelectionListener(new ComboValuesSelectionListener() {
      public String[] getComboValues(TableItem tableItem, int rowNr, int colNr) {
        String[] comboValues = null;
        
        String keyOrNot = tableItem.getText(2);
        if (Const.isEmpty(keyOrNot) || keyOrNot.equalsIgnoreCase("N")) {
          comboValues = new String[] {"String", "Integer", "Long", "Float", 
              "Double", "Boolean", "Date", "BigNumber", "Serializable", "Binary"};
        } else {
          comboValues = new String[] {"String", "Integer", "UnsignedInteger", 
              "Long", "UnsignedLong", "Date", "UnsignedDate"};
        }
        
        return comboValues;
      }
    });
    
    m_saveBut = new Button(this, SWT.PUSH | SWT.CENTER);
    props.setLook(m_saveBut);
    m_saveBut.setText(Messages.getString("MappingDialog.SaveMapping"));
    fd = new FormData();
    fd.left = new FormAttachment(0, margin);
    fd.bottom = new FormAttachment(100, -margin*2);
    m_saveBut.setLayoutData(fd);
    
    m_saveBut.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        saveMapping();
      }
    });
    
    m_deleteBut = new Button(this, SWT.PUSH | SWT.CENTER);
    props.setLook(m_deleteBut);
    m_deleteBut.setText(Messages.getString("MappingDialog.DeleteMapping"));
    fd = new FormData();
    fd.left = new FormAttachment(m_saveBut, margin);
    fd.bottom = new FormAttachment(100, -margin*2);
    m_deleteBut.setLayoutData(fd);
    m_deleteBut.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        deleteMapping();
      }
    });
    
    if (m_allowTableCreate) {
      m_getFieldsBut = new Button(this, SWT.PUSH | SWT.CENTER);
      props.setLook(m_getFieldsBut);
      m_getFieldsBut.setText(Messages.getString("MappingDialog.GetIncomingFields"));
      fd = new FormData();
      //fd.left = new FormAttachment(0, margin);
      fd.right = new FormAttachment(100, 0);
      fd.bottom = new FormAttachment(100, -margin*2);
      m_getFieldsBut.setLayoutData(fd);
      
      m_getFieldsBut.addSelectionListener(new SelectionAdapter() {
        public void widgetSelected(SelectionEvent e) {
          populateTableWithIncomingFields();
        }
      });
    }
    
    
    
    m_fieldsView = new TableView(transMeta, this, 
        tableViewStyle,
        colinf, 1, null, props);
                        
    fd = new FormData();
    fd.top   = new FormAttachment(m_existingMappingNamesCombo, margin*2);
    fd.bottom= new FormAttachment(m_saveBut, -margin*2);
    fd.left  = new FormAttachment(0, 0);
    fd.right = new FormAttachment(100, 0);
    m_fieldsView.setLayoutData(fd);
    
    
    // --
    //layout();
    //pack();    
  }
  
  private void populateTableWithIncomingFields() {
    if (m_incomingFieldsProducer != null) {
      RowMetaInterface incomingRowMeta = m_incomingFieldsProducer.getIncomingFields();
      
      Table table = m_fieldsView.table;
      if (incomingRowMeta != null) {
        Set<String> existingRowAliases = new HashSet<String>();
        for (int i = 0; i < table.getItemCount(); i++) {
          TableItem tableItem = table.getItem(i);
          String alias = tableItem.getText(1);
          if (!Const.isEmpty(alias)) {
            existingRowAliases.add(alias);
          }
        }
        
        int choice = 0;
        if (existingRowAliases.size() > 0) {
          // Ask what we should do with existing mapping data
          MessageDialog md = new MessageDialog(m_shell, 
              Messages.getString("MappingDialog.GetFieldsChoice.Title"),
              null, Messages.getString("MappingDialog.GetFieldsChoice.Message", 
                  "" + existingRowAliases.size(), 
                  "" + incomingRowMeta.size()),
              MessageDialog.WARNING, 
              new String[] { Messages.getString("MappingDialog.AddNew"),
                  Messages.getString("MappingOutputDialog.Add"), 
                  Messages.getString("MappingOutputDialog.ClearAndAdd"),
                  Messages.getString("MappingOutputDialog.Cancel"), }, 
                  0);
          MessageDialog.setDefaultImage(GUIResource.getInstance().getImageSpoon());
          int idx = md.open();
          choice = idx & 0xFF;
        }
        
        if (choice == 3 || choice == 255 /* 255 = escape pressed */) {
          return; // Cancel
        }
        
        if (choice == 2) {
          m_fieldsView.clearAll();
        }
        
        for (int i = 0; i < incomingRowMeta.size(); i++) {
          ValueMetaInterface vm = incomingRowMeta.getValueMeta(i);
          boolean addIt = true;
          
          if (choice == 0) {
            // only add if its not already in the table
            if (existingRowAliases.contains(vm.getName())) {
              addIt = false;
            }
          }
          
          if (addIt) {
            TableItem item = new TableItem(m_fieldsView.table, SWT.NONE);
            item.setText(1, vm.getName());
            item.setText(2, "N");
            
            if (m_familyCI.getComboValues()[0].length() > 0) {
              // use existing first column family name as the default
              item.setText(3, m_familyCI.getComboValues()[0]);
            } else {
              // default
              item.setText(3, DEFAULT_FAMILY);
            }
            
            item.setText(4, vm.getName());
            item.setText(5, vm.getTypeDesc());
            if (vm.getType() == ValueMetaInterface.TYPE_INTEGER) {
              item.setText(5, "Long");
            }
            if (vm.getType() == ValueMetaInterface.TYPE_NUMBER) {
              item.setText(5, "Double");
            }
            if (vm.getStorageType() == ValueMetaInterface.STORAGE_TYPE_INDEXED) {
              Object[] indexValus = vm.getIndex();
              String indexValsS = HBaseValueMeta.objectIndexValuesToString(indexValus);
              item.setText(6, indexValsS);
            }
          }
        }

        m_fieldsView.removeEmptyRows();
        m_fieldsView.setRowNums();
        m_fieldsView.optWidth(true);
      }
    }
  }
  
  private void populateTableCombo(boolean force) {

    if (m_configProducer == null) {
      return;
    }

    if (m_existingTableNamesCombo.getItemCount() == 0 || force) {
      String existingName = m_existingTableNamesCombo.getText();
      m_existingTableNamesCombo.removeAll();
      //m_existingMappingNamesCombo.removeAll();
      try  {
        Configuration conf = m_configProducer.getHBaseConnection();
        m_admin = new MappingAdmin();
        m_admin.setConnection(conf);

        HBaseAdmin admin = new HBaseAdmin(conf);
        HTableDescriptor[] tables = admin.listTables();

        for (int i = 0; i < tables.length; i++) {
          String currentTableName = tables[i].getNameAsString();
          m_existingTableNamesCombo.add(currentTableName);
        }
        // restore any previous value
        if (!Const.isEmpty(existingName)) {
          m_existingTableNamesCombo.setText(existingName);
        }
      } catch (Exception ex) {
        // ignore quietly
      }
    }
  }
  
  private void deleteMapping() {
    if (Const.isEmpty(m_existingTableNamesCombo.getText().trim()) ||
        Const.isEmpty(m_existingMappingNamesCombo.getText().trim())) {
      MessageDialog.openError(m_shell, 
          Messages.getString("MappingDialog.Error.Title.MissingTableMappingName"),
          Messages.getString("MappingDialog.Error.Message.MissingTableMappingName"));
      return;
    }
    
    try {
      boolean ok = MessageDialog.openConfirm(m_shell, 
          Messages.getString("MappingDialog.Info.Title.ConfirmDelete"), 
          Messages.getString("MappingDialog.Info.Message.ConfirmDelete",
              m_existingMappingNamesCombo.getText().trim(), 
              m_existingTableNamesCombo.getText().trim()));
      
      if (ok) {
        boolean result = m_admin.deleteMapping(m_existingTableNamesCombo.getText().trim(), 
            m_existingMappingNamesCombo.getText().trim());
        if (result) {
          MessageDialog.openConfirm(m_shell,
              Messages.getString("MappingDialog.Info.Title.MappingDeleted"),
              Messages.getString("MappingDialog.Info.Message.MappingDeleted",
                  m_existingMappingNamesCombo.getText().trim(), 
                  m_existingTableNamesCombo.getText().trim()));

          // make sure that the list of mappings for the selected table gets updated.
          populateMappingComboAndFamilyStuff();
        } else {
          MessageDialog.openError(m_shell, 
              Messages.getString("MappingDialog.Error.Title.DeleteMapping"),
              Messages.getString("MappingDialog.Error.Message.DeleteMapping", 
                  m_existingMappingNamesCombo.getText().trim(), 
                  m_existingTableNamesCombo.getText().trim()));
        }
      }
      return;
    } catch (IOException ex) {
      MessageDialog.openError(m_shell, 
          Messages.getString("MappingDialog.Error.Title.DeleteMapping"),
          Messages.getString("MappingDialog.Error.Message.DeleteMappingIO", 
              m_existingMappingNamesCombo.getText().trim(), 
              m_existingTableNamesCombo.getText().trim(), ex.getMessage()));
    }
  }
  
  private void saveMapping() {
    
    if (Const.isEmpty(m_existingTableNamesCombo.getText().trim()) ||
        Const.isEmpty(m_existingMappingNamesCombo.getText().trim())) {
      MessageDialog.openError(m_shell, 
          Messages.getString("MappingDialog.Error.Title.MissingTableMappingName"),
          Messages.getString("MappingDialog.Error.Message.MissingTableMappingName"));
      return;
    }
    
    // do we have any non-empty rows in the table?
    if (m_fieldsView.nrNonEmpty() == 0) {
      MessageDialog.openError(m_shell, 
          Messages.getString("MappingDialog.Error.Title.NoFieldsDefined"), 
          Messages.getString("MappingDialog.Error.Message.NoFieldsDefined"));
      return;
    }
    // do we have a key defined in the table?
    Mapping theMapping = new Mapping(m_existingTableNamesCombo.getText().trim(),
        m_existingMappingNamesCombo.getText().trim());
    boolean keyDefined = false;
    boolean moreThanOneKey = false;
    List<String> missingFamilies = new ArrayList<String>();
    List<String> missingColumnNames = new ArrayList<String>();
    List<String> missingTypes = new ArrayList<String>();
    
    int nrNonEmpty = m_fieldsView.nrNonEmpty();
    for (int i = 0; i < nrNonEmpty; i++) {
      TableItem item = m_fieldsView.getNonEmpty(i);
      boolean isKey = false;
      String alias = null;
      if (!Const.isEmpty(item.getText(1))) {
        alias = item.getText(1).trim();
      }
      if (!Const.isEmpty(item.getText(2))) {
        isKey = item.getText(2).trim().equalsIgnoreCase("Y");
        
        if (isKey && keyDefined) {
          // more than one key, break here
          moreThanOneKey = true;
          break;
        }
        if (isKey) {
          keyDefined = true;
        }
      }
      String family = null;
      if (!Const.isEmpty(item.getText(3))) {
        family = item.getText(3);
      } else {
        if (!isKey) {
          missingFamilies.add(item.getText(0));
        }
      }
      String colName = null;
      if (!Const.isEmpty(item.getText(4))) {
        colName = item.getText(4);
      } else {
        if (!isKey) {
          missingColumnNames.add(item.getText(0));
        }
      }
      String type = null;
      if (!Const.isEmpty(item.getText(5))) {
        type = item.getText(5);
      } else {
        missingTypes.add(item.getText(0));
      }
      String indexedVals = null;
      if (!Const.isEmpty(item.getText(6))) {
        indexedVals = item.getText(6);
      }/* else {
        if (!isKey && type != null && type.equalsIgnoreCase("String")) {
          missingIndexedValues.add(item.getText(0));
        }
      }*/
      
      // only add if we have all data and its all correct
      if (isKey && !moreThanOneKey) {
        if (Const.isEmpty(alias)) {
          // pop up an error dialog - key must have an alias because it does not
          // belong to a column family or have a column name
          MessageDialog.openError(m_shell, 
              Messages.getString("MappingDialog.Error.Title.NoAliasForKey"),
              Messages.getString("MappingDialog.Error.Message.NoAliasForKey"));
          return;
        }
        
        if (Const.isEmpty(type)) {
          // pop up an error dialog - must have a type for the key
          MessageDialog.openError(m_shell, 
              Messages.getString("MappingDialog.Error.Title.NoTypeForKey"),
              Messages.getString("MappingDialog.Error.Message.NoTypeForKey"));
          return;
        }
        
        if (moreThanOneKey) {
          // popup an error and then return
          MessageDialog.openError(m_shell, 
              Messages.getString("MappingDialog.Error.Title.MoreThanOneKey"),
              Messages.getString("MappingDialog.Error.Message.MoreThanOneKey"));
          return;
        }
        
        theMapping.setKeyName(alias);
        try {
          theMapping.setKeyTypeAsString(type);
        } catch (Exception ex) {
          
        }
      } else {
        // don't bother adding if there are any errors
        if (missingFamilies.size() == 0 && missingColumnNames.size() == 0 && 
            missingTypes.size() == 0) {
          String combinedName = family + HBaseValueMeta.SEPARATOR + colName;
          if (!Const.isEmpty(alias)) {
            combinedName += (HBaseValueMeta.SEPARATOR + alias);
          }
          HBaseValueMeta vm = new HBaseValueMeta(combinedName, 0, -1, -1);
          try {
            vm.setHBaseTypeFromString(type);
          } catch (IllegalArgumentException e) {
            // TODO pop up an error dialog for this one
            return;
          }
          if (vm.isString() && indexedVals != null && indexedVals.length() > 0) {
            Object[] vals = HBaseValueMeta.stringIndexListToObjects(indexedVals);
            vm.setIndex(vals);
            vm.setStorageType(ValueMetaInterface.STORAGE_TYPE_INDEXED);
          }
          
          try {
            theMapping.addMappedColumn(vm);
          } catch (Exception ex) {
            // popup an error if this family:column is already in the mapping and
            // then return.
            MessageDialog.openError(m_shell, 
                Messages.getString("MappingDialog.Error.Title.DuplicateColumn"), 
                Messages.getString("MappingDialog.Message1.Title.DuplicateColumn") +
                family + HBaseValueMeta.SEPARATOR + colName + 
                Messages.getString("MappingDialog.Message1.Title.DuplicateColumn"));
            ex.printStackTrace();
            return;
          }
        }
      }
    }
    
    // now check for any errors in our Lists
    if (!keyDefined) {
      MessageDialog.openError(m_shell,
          Messages.getString("MappingDialog.Error.Title.NoKeyDefined"), 
          Messages.getString("MappingDialog.Error.Message.NoKeyDefined"));
      return;
    }
    
    if (missingFamilies.size() > 0 || missingColumnNames.size() > 0 || 
        missingTypes.size() > 0) {
      StringBuffer buff = new StringBuffer();
      buff.append(Messages.
          getString("MappingDialog.Error.Message.IssuesPreventingSaving") + ":\n\n");
      if (missingFamilies.size() > 0) {
        buff.append(Messages.getString("MappingDialog.Error.Message.FamilyIssue") + ":\n");
        buff.append(missingFamilies.toString()).append("\n\n");
      }
      if (missingColumnNames.size() > 0) {
        buff.append(Messages.getString("MappingDialog.Error.Message.ColumnIssue") + ":\n");
        buff.append(missingColumnNames.toString()).append("\n\n");
      }
      if (missingTypes.size() > 0) {
        buff.append(Messages.getString("MappingDialog.Error.Message.TypeIssue") + ":\n");
        buff.append(missingTypes.toString()).append("\n\n");
      }
      
      MessageDialog.openError(m_shell, 
          Messages.getString("MappingDialog.Error.Title.IssuesPreventingSaving"), 
          buff.toString());
      return;
    }
    
    
    if (m_allowTableCreate) {
      // check for existence of the table. If table doesn't exist
      // prompt for creation
      Configuration conf = m_admin.getConnection();

      try {
        HBaseAdmin admin = new HBaseAdmin(conf);
        String tableName = m_existingTableNamesCombo.getText().trim();
        if (!admin.tableExists(tableName)) {
          boolean result = MessageDialog.openConfirm(m_shell, "Create table", 
              "Table \"" + tableName + "\" does not exist. Create it?");
          
          if (!result) {
            return;
          }
          
          if (theMapping.getMappedColumns().size() == 0) {
            MessageDialog.openError(m_shell, "No columns defined", 
                "A HBase table requires at least one column family to be defined.");
            return;
          }
          
          // collect up all the column families so that we can create the table
          Set<String> cols = theMapping.getMappedColumns().keySet();
          Set<String> families = new TreeSet<String>();
          for (String col : cols) {
            String family = theMapping.getMappedColumns().get(col).getColumnFamily();
            families.add(family);
          }
          
          HTableDescriptor tableDescription = new HTableDescriptor(tableName);
          for (String familyName : families) {
            HColumnDescriptor colDescriptor = new HColumnDescriptor(familyName);
            tableDescription.addFamily(colDescriptor);
          }
          
          // create the table
          admin.createTable(tableDescription);
          
          // refresh the table combo
          populateTableCombo(true);
        }
      } catch (IOException ex) {
        new ErrorDialog(m_shell, 
            Messages.getString("MappingDialog.Error.Title.ErrorCreatingTable"), 
            Messages.getString("MappingDialog.Error.Message.ErrorCreatingTable")
            + " \"" + m_existingTableNamesCombo.getText().trim() + "\"", ex);
        return;
      }
    }

    try {
      // now check to see if the mapping exists
      if (m_admin.mappingExists(m_existingTableNamesCombo.getText().trim(), 
          m_existingMappingNamesCombo.getText().trim())) {
        // prompt for overwrite
        boolean result = 
          MessageDialog.openConfirm(m_shell,
              Messages.getString("MappingDialog.Info.Title.MappingExists"),
              Messages.getString("MappingDialog.Info.Message1.MappingExists") 
            + m_existingMappingNamesCombo.getText().trim() + 
            Messages.getString("MappingDialog.Info.Message2.MappingExists")
            + m_existingTableNamesCombo.getText().trim() 
            + Messages.getString("MappingDialog.Info.Message3.MappingExists"));
        if (!result) {
          return;
        }
        
        // TODO possibly check for consistency against any other mappings
        // for this table?
        
      }
      // finally add the mapping.
      m_admin.putMapping(theMapping, true);
      MessageDialog.openConfirm(m_shell,
          Messages.getString("MappingDialog.Info.Title.MappingSaved"),
          Messages.getString("MappingDialog.Info.Message1.MappingSaved")
          + m_existingMappingNamesCombo.getText().trim() 
          + Messages.getString("MappingDialog.Info.Message2.MappingSaved")
          + m_existingTableNamesCombo.getText().trim() 
          + Messages.getString("MappingDialog.Info.Message3.MappingSaved"));
    } catch (IOException ex) {
      // inform the user via popup
      new ErrorDialog(m_shell, 
          Messages.getString("MappingDialog.Error.Title.ErrorSaving"), 
          Messages.getString("MappingDialog.Error.Message.ErrorSaving"), ex);
      ex.printStackTrace();
    }
  }
  
  private void loadTableViewFromMapping() {
    
    try {
      if (m_admin.mappingExists(m_existingTableNamesCombo.getText().trim(), 
          m_existingMappingNamesCombo.getText().trim())) {

        Mapping mapping = 
          m_admin.getMapping(m_existingTableNamesCombo.getText().trim(), 
              m_existingMappingNamesCombo.getText().trim());
        m_fieldsView.clearAll();
        
        // do the key first
        TableItem keyItem = new TableItem(m_fieldsView.table, SWT.NONE);
        keyItem.setText(1, mapping.getKeyName());
        keyItem.setText(2, "Y");
        keyItem.setText(5, mapping.getKeyType().toString());
        
        // the rest of the fields in the mapping
        Map<String, HBaseValueMeta> mappedFields = mapping.getMappedColumns();
        for (String alias : mappedFields.keySet()) {
          HBaseValueMeta vm = mappedFields.get(alias);
          TableItem item = new TableItem(m_fieldsView.table, SWT.NONE);
          item.setText(1, alias);
          item.setText(2, "N");
          item.setText(3, vm.getColumnFamily());
          item.setText(4, vm.getColumnName());
          
          if (vm.isInteger()) {
            if (vm.getIsLongOrDouble()) {
              item.setText(5, "Long");
            } else {
              item.setText(5, "Integer");
            }
          } else if (vm.isNumber()) {
            if (vm.getIsLongOrDouble()) {
              item.setText(5, "Double");
            } else {
              item.setText(5, "Float");
            }
          }  else {
            item.setText(5, vm.getTypeDesc());
          }
          
          if (vm.getStorageType() == ValueMetaInterface.STORAGE_TYPE_INDEXED) {
            item.setText(6, HBaseValueMeta.objectIndexValuesToString(vm.getIndex()));
          }
        }
        
        m_fieldsView.removeEmptyRows();
        m_fieldsView.setRowNums();
        m_fieldsView.optWidth(true);
      }

    } catch (IOException ex) {
      // inform the user via popup
      new ErrorDialog(m_shell, 
          Messages.getString("MappingDialog.Error.Title.ErrorLoadingMapping"),
          Messages.getString("MappingDialog.Error.Message.ErrorLoadingMapping"), ex);
      ex.printStackTrace();
    }
  }
  
  private void populateMappingComboAndFamilyStuff() {
    
    // defaults if we fail to connect, table doesn't exist etc..
    m_familyCI.setComboValues(new String[] {""});
    m_existingMappingNamesCombo.removeAll();
    
    if (m_admin != null && !Const.isEmpty(m_existingTableNamesCombo.getText())) {
      try {
        
        // first get the existing mapping names (if any)
        List<String> mappingNames = 
          m_admin.getMappingNames(m_existingTableNamesCombo.getText().trim());
        for (String m : mappingNames) {
          m_existingMappingNamesCombo.add(m);
        }
        
        // now get family information for this table
        Configuration conf = m_admin.getConnection();
        HBaseAdmin admin = new HBaseAdmin(conf);
        
        if (admin.tableExists(m_existingTableNamesCombo.getText())) {
          HTableDescriptor descriptor = 
            admin.getTableDescriptor(Bytes.toBytes(m_existingTableNamesCombo.getText().trim()));
          
          Collection<HColumnDescriptor> families = descriptor.getFamilies();
          String[] familyNames = new String[families.size()];
          int i = 0;
          for (HColumnDescriptor d : families) {
            familyNames[i++] = d.getNameAsString();            
          }
          
          m_familyCI.setComboValues(familyNames);          
        } else {
          m_familyCI.setComboValues(new String[] {""});
        }
        
        m_familiesInvalidated = false;
        return;
        
      } catch (Exception e) {
        // TODO popup error dialog
        e.printStackTrace();
      }
    }    
  }
}
