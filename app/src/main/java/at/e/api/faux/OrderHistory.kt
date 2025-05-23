package at.e.api.faux

import at.e.api.Order

class OrderHistory(val order: Order, val suborders: List<SuborderState>)
