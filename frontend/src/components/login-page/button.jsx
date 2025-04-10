import React from "react"

export function Button({ children, className, ...props }) {
  return (
    <button
      className={`px-4 py-2 bg-primary text-white rounded hover:bg-primary-dark disabled:opacity-50 ${className}`}
      {...props}
    >
      {children}
    </button>
  )
}