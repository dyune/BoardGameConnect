import * as React from "react";
import { cn } from "../lib/utils";

// Simple tooltip component
const TooltipContext = React.createContext(null);

export function Tooltip({ children, className }) {
  const [isOpen, setIsOpen] = React.useState(false);
  
  // Close tooltip when clicking outside
  React.useEffect(() => {
    if (!isOpen) return;
    
    const handleClickOutside = (e) => {
      setIsOpen(false);
    };
    
    document.addEventListener('click', handleClickOutside);
    return () => document.removeEventListener('click', handleClickOutside);
  }, [isOpen]);
  
  return (
    <TooltipContext.Provider value={{ isOpen, setIsOpen }}>
      <div className="relative inline-block" onClick={(e) => e.stopPropagation()}>{children}</div>
    </TooltipContext.Provider>
  );
}

export function TooltipTrigger({ children, className, asChild = false }) {
  const { setIsOpen } = React.useContext(TooltipContext);

  const handleMouseEnter = () => setIsOpen(true);
  const handleMouseLeave = () => setIsOpen(false);
  const handleClick = (e) => {
    e.stopPropagation();
    setIsOpen(prev => !prev);
  };

  const Comp = asChild ? React.cloneElement(children, {
    onMouseEnter: handleMouseEnter,
    onMouseLeave: handleMouseLeave,
    onClick: handleClick,
    onTouchEnd: handleClick
  }) : (
    <div
      onMouseEnter={handleMouseEnter}
      onMouseLeave={handleMouseLeave}
      onClick={handleClick}
      onTouchEnd={handleClick}
      className={className}
    >
      {children}
    </div>
  );

  return Comp;
}

export function TooltipContent({ children, className, sideOffset = 4 }) {
  const { isOpen } = React.useContext(TooltipContext);
  
  if (!isOpen) return null;

  return (
    <div
      className={cn(
        "absolute z-50 top-full mt-2 max-w-xs",
        "px-3 py-1.5 rounded-md shadow-md",
        "bg-background border border-border",
        "text-sm text-foreground animate-in fade-in-0 zoom-in-95",
        className
      )}
      style={{ left: "50%", transform: "translateX(-50%)" }}
      onClick={(e) => e.stopPropagation()}
    >
      {children}
      <div className="absolute -top-1 left-1/2 -translate-x-1/2 h-2 w-2 rotate-45 bg-background border-t border-l border-border" />
    </div>
  );
}

// Add a new component specifically for tooltips that appear above elements
export function TopTooltipContent({ children, className }) {
  const { isOpen } = React.useContext(TooltipContext);
  
  if (!isOpen) return null;

  return (
    <div
      className={cn(
        "absolute z-50 bottom-[calc(100%+10px)] max-w-xs",
        "px-3 py-1.5 rounded-md shadow-md",
        "bg-background border border-border",
        "text-sm text-foreground animate-in fade-in-0 zoom-in-95",
        className
      )}
      style={{ left: "50%", transform: "translateX(-50%)" }}
      onClick={(e) => e.stopPropagation()}
    >
      {children}
    </div>
  );
} 