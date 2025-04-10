import { useState } from "react";

export function DateFilterComponent({ onFilterChange }) {
  const [dateFilter, setDateFilter] = useState("all");
  
  const handleDateFilterChange = (e) => {
    const filterValue = e.target.value;
    setDateFilter(filterValue);
    onFilterChange(filterValue);
  };
  
  return (
    <div className="w-full">
      <select
        className="w-full p-2 border rounded-lg bg-background text-foreground border-input focus:outline-none focus:ring-2 focus:ring-ring"
        value={dateFilter}
        onChange={handleDateFilterChange}
      >
        <option value="all">All Dates</option>
        <option value="this-week">This week</option>
        <option value="this-month">This month</option>
        <option value="next-month">Next month</option>
      </select>
    </div>
  );
}