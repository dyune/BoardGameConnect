import React from "react"

export function Input({ className, ...props }) {
  return (
    <input
      className={`w-full px-3 py-2 border rounded focus:outline-none focus:ring focus:ring-primary ${className}`}
      {...props}
    />
  )
}