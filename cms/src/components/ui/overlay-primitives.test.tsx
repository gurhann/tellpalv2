import { fireEvent, render, screen } from "@testing-library/react";
import { useState } from "react";

import {
  Dialog,
  DialogBody,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  Sheet,
  SheetBody,
  SheetContent,
  SheetFooter,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet";

describe("Overlay primitives", () => {
  it("keeps dialog header and footer outside the scrollable body", () => {
    function DialogHarness() {
      return (
        <Dialog open>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>Dialog title</DialogTitle>
            </DialogHeader>
            <DialogBody>
              <div>Scrollable body</div>
            </DialogBody>
            <DialogFooter>
              <button type="button">Save</button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      );
    }

    render(<DialogHarness />);

    const dialog = screen.getByRole("dialog", { name: /dialog title/i });
    const body = screen
      .getByText("Scrollable body")
      .closest("[data-slot='dialog-body']");
    const footer = screen.getByRole("button", { name: /save/i }).closest(
      "[data-slot='dialog-footer']",
    );

    expect(dialog).toHaveClass(
      "max-h-[calc(100dvh-1.5rem)]",
      "min-h-0",
      "flex",
      "flex-col",
      "overflow-hidden",
    );
    expect(body).toHaveClass("min-h-0", "overflow-y-auto", "px-5", "pb-5");
    expect(footer).toHaveClass(
      "shrink-0",
      "border-t",
      "bg-muted/45",
      "px-5",
      "py-4",
    );
  });

  it("closes dialogs on escape after focus moves inside the overlay", () => {
    function ControlledDialogHarness() {
      const [open, setOpen] = useState(true);

      return (
        <Dialog open={open} onOpenChange={setOpen}>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>Escape dialog</DialogTitle>
            </DialogHeader>
            <DialogBody>
              <button type="button">Focusable action</button>
            </DialogBody>
          </DialogContent>
        </Dialog>
      );
    }

    render(<ControlledDialogHarness />);

    const focusableAction = screen.getByRole("button", {
      name: /focusable action/i,
    });

    focusableAction.focus();
    fireEvent.keyDown(document, { key: "Escape" });

    expect(
      screen.queryByRole("dialog", { name: /escape dialog/i }),
    ).not.toBeInTheDocument();
  });

  it("renders sheet drawers with a dedicated scroll body and footer", () => {
    render(
      <Sheet open>
        <SheetContent side="right">
          <SheetHeader>
            <SheetTitle>Drawer title</SheetTitle>
          </SheetHeader>
          <SheetBody>
            <div>Drawer body</div>
          </SheetBody>
          <SheetFooter>
            <button type="button">Apply</button>
          </SheetFooter>
        </SheetContent>
      </Sheet>,
    );

    const drawer = screen.getByRole("dialog", { name: /drawer title/i });
    const body = screen.getByText("Drawer body").closest("[data-slot='sheet-body']");
    const footer = screen.getByRole("button", { name: /apply/i }).closest(
      "[data-slot='sheet-footer']",
    );

    expect(drawer).toHaveClass("data-[side=right]:grid-rows-[auto_minmax(0,1fr)_auto]");
    expect(body).toHaveClass("min-h-0", "overflow-y-auto", "px-5", "pb-5");
    expect(footer).toHaveClass("shrink-0", "border-t", "bg-muted/45");
  });
});
