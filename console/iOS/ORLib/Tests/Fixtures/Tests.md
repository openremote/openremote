### Test0

DefaultConfig, nothing present

### Test1

ConsoleConfig not asking for app or realm, no apps list -> defaults to manager, no realm

### Test2

ConsoleConfig with a specified app

### Test3

ConsoleConfig with a specified app but requesting the text field to enter an app name -> should still use the specified app

### Test4

ConsoleConfig with no specified app and a list of apps

### Test5

ConsoleConfig with no specified app and a list of apps but requesting the text field to enter an app name

### Test6

ConsoleConfig with no specified app and no allowed apps but apps list available via Apps call

### Test 7

Similar to Test6 but with a list of realm defined for the selected app

### Test 8

Similar to Test7 but with the realms defined for the default app


TODO:
- add test for full URLs entry
