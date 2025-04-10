import React, { useState } from 'react';
import Tag from './Tag.jsx';
import { Tooltip, TooltipTrigger, TooltipContent } from '../ui/tooltip.jsx';
import { 
  Dialog, 
  DialogContent, 
  DialogHeader, 
  DialogTitle, 
  DialogDescription,
  DialogClose 
} from '../ui/dialog.jsx';
import { XIcon } from 'lucide-react';

/**
 * A special Player tag with a tooltip explaining what it means
 */
const PlayerTag = ({ className }) => {
  const [showFullDialog, setShowFullDialog] = useState(false);

  // Handler for when the tag is clicked
  const handleTagClick = (e) => {
    // Ensure the click doesn't propagate to parent elements
    e.stopPropagation();
    e.preventDefault();
    setShowFullDialog(true);
  };

  // Handler for when the dialog is closed
  const handleDialogClose = (open) => {
    // When dialog is closed, make sure to prevent the event from reaching the card
    if (!open) {
      // Small delay to allow the dialog close animation to complete
      // before setting the state, which helps prevent race conditions
      setTimeout(() => {
        setShowFullDialog(false);
      }, 10);
    }
  };

  // This function is crucial - it prevents any click inside the dialog
  // from propagating to parent elements like the card
  const blockPropagation = (e) => {
    // Using stopPropagation and preventDefault to ensure the click doesn't bubble up
    e.stopPropagation();
    e.preventDefault();
  };

  return (
    <div onClick={blockPropagation}>
      <Tooltip>
        <TooltipTrigger asChild>
          <div className="inline-block cursor-help" onClick={blockPropagation}>
            <Tag 
              text="Player" 
              variant="player" 
              className={className} 
              interactive={true}
              onClick={handleTagClick}
            />
          </div>
        </TooltipTrigger>
        <TooltipContent className="text-xs py-1 px-2" onClick={blockPropagation}>
          Click for more information
        </TooltipContent>
      </Tooltip>

      {/* Full dialog with detailed information */}
      <Dialog 
        open={showFullDialog} 
        onOpenChange={handleDialogClose}
      >
        <DialogContent className="sm:max-w-md overflow-hidden" onClick={blockPropagation}>
          {/* Simple close button without additional handlers */}
          <DialogClose 
            className="absolute top-4 right-4 rounded-sm opacity-70 ring-offset-background transition-opacity hover:opacity-100 focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 disabled:pointer-events-none"
            onClick={blockPropagation}
          >
            <XIcon className="h-4 w-4" />
            <span className="sr-only">Close</span>
          </DialogClose>

          {/* Content area with propagation blocking */}
          <div onClick={blockPropagation} className="select-none">
            <DialogHeader>
              <DialogTitle className="text-xl text-center text-blue-800">
                What is a Player?
              </DialogTitle>
              <DialogDescription className="text-center">
                Players are members who engage with the gaming community
              </DialogDescription>
            </DialogHeader>
            
            <div className="space-y-4 py-2">
              <p>
                Players can borrow games from Game Owners, participate in gaming events, 
                and connect with other game enthusiasts in the community.
              </p>

              <div>
                <h4 className="font-medium mb-2">As a Player, you can:</h4>
                <ul className="list-disc pl-5 space-y-2">
                  <li>Borrow games from the community collection</li>
                  <li>Join various gaming events and meet new friends</li>
                  <li>Rate games you've played and leave reviews</li>
                  <li>Upgrade to Game Owner status anytime</li>
                </ul>
              </div>

              <div className="bg-blue-50 p-4 rounded-md">
                <p className="font-medium text-center">
                  Want to share your own games? Consider becoming a Game Owner in your User Settings.
                </p>
              </div>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
};

export default PlayerTag; 