import 'package:event_bus/event_bus.dart';

class EventBusService {
  static EventBusService _instance;

  EventBus eventBus;

  EventBusService._internal() {
    eventBus = EventBus();
  }

  static EventBusService getInstance() {
    return _instance ??= EventBusService._internal();
  }
}
