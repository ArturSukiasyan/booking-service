
-- Individual indexes to support selective filters and sorting
CREATE INDEX IF NOT EXISTS idx_units_type_only ON units(type);
CREATE INDEX IF NOT EXISTS idx_units_rooms_only ON units(rooms);
CREATE INDEX IF NOT EXISTS idx_units_floor_only ON units(floor);
CREATE INDEX IF NOT EXISTS idx_units_base_cost_only ON units(base_cost);

CREATE INDEX IF NOT EXISTS idx_bookings_unit_only ON bookings(unit_id);
CREATE INDEX IF NOT EXISTS idx_bookings_status_only ON bookings(status);
CREATE INDEX IF NOT EXISTS idx_bookings_start_date_only ON bookings(start_date);
CREATE INDEX IF NOT EXISTS idx_bookings_end_date_only ON bookings(end_date);
