Macro used for inserting a row below the current selection (very convenient when bound to a key, e.g Alt + Insert)
```basic
Sub CopyRangeInsertAfter()
REM Blablabla
	Dim oSheet, oSelection, oSource, oTarget
	oSheet = ThisComponent.CurrentController.ActiveSheet
	oSelection = ThisComponent.getCurrentSelection()
	oSource = oSelection.getRangeAddress()
	oSheet.Rows.insertByIndex(oSource.EndRow + 1, 1 + oSource.EndRow - oSource.StartRow)
	oTarget = oSheet.getCellByPosition(oSource.StartColumn, oSource.EndRow + 1).CellAddress
	oSheet.copyRange(oTarget, oSource)
End Sub
```