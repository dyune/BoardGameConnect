import React from "react"

export function Card({ children, className }) {
  return <div className={`border rounded shadow-md p-4 ${className}`}>{children}</div>
}

export function CardHeader({ children, className }) {
  return <div className={`mb-4 ${className}`}>{children}</div>
}

export function CardContent({ children, className }) {
  return <div className={`mb-4 ${className}`}>{children}</div>
}

export function CardFooter({ children, className }) {
  return <div className={`mt-4 ${className}`}>{children}</div>
}

export function CardTitle({ children, className }) {
  return <h2 className={`text-lg font-bold ${className}`}>{children}</h2>
}

export function CardDescription({ children, className }) {
  return <p className={`text-sm text-gray-600 ${className}`}>{children}</p>
}