import React from 'react';
import { HelpCircle } from 'lucide-react';
import {
  Tooltip,
  TopTooltipContent,
  TooltipTrigger,
} from './tooltip';

export const ReviewTooltip = ({ children }) => {
  return (
    <Tooltip>
      <TooltipTrigger asChild>
        <div className="inline-flex items-center">
          {children}
          <HelpCircle className="h-3.5 w-3.5 ml-1 text-muted-foreground" />
        </div>
      </TooltipTrigger>
      <TopTooltipContent className="bg-popover p-3 max-w-[280px] rounded-lg shadow-lg border border-border">
        <div className="text-xs font-medium mb-1 text-primary">Review Requirements</div>
        <p className="text-xs text-muted-foreground">
          You can only review games that you have previously borrowed and returned.
        </p>
      </TopTooltipContent>
    </Tooltip>
  );
}; 