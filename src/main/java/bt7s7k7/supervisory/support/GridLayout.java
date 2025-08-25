package bt7s7k7.supervisory.support;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.client.gui.components.AbstractWidget;

public class GridLayout {
	public static class Builder {
		protected final List<Integer> columns = new ArrayList<>();
		protected final List<Integer> rows = new ArrayList<>();
		protected int gap = 0;
		protected int offsetX = 0;
		protected int offsetY = 0;

		protected int limitX = 0;
		protected int limitY = 0;

		protected int growColumns = 0;
		protected int growRows = 0;

		protected final List<Consumer<GridLayout>> deferredActions = new ArrayList<>();

		public Builder setGap(int gap) {
			this.gap = gap;
			return this;
		}

		public Builder addColumn(int size) {
			this.columns.add(size);
			return this;
		}

		public Builder addColumns(int size, int count) {
			for (int i = 0; i < count; i++) {
				this.columns.add(size);
			}

			return this;
		}

		public Builder addColumn() {
			this.columns.add(this.limitX);
			return this;
		}

		public Builder addGrowColumn() {
			this.growColumns++;
			this.columns.add(Integer.MIN_VALUE);
			return this;
		}

		public Builder addRow(int size) {
			this.rows.add(size);
			return this;
		}

		public Builder addRows(int size, int count) {
			for (int i = 0; i < count; i++) {
				this.rows.add(size);
			}

			return this;
		}

		public Builder addRow() {
			this.rows.add(this.limitY);
			return this;
		}

		public Builder addGrowRow() {
			this.growRows++;
			this.rows.add(Integer.MIN_VALUE);
			return this;
		}

		public int getWidth() {
			if (this.columns.isEmpty()) return 0;
			return columns.stream().filter(v -> v > 0).mapToInt(Integer::intValue).sum() + this.gap * (this.columns.size() - 1);
		}

		public int getHeight() {
			if (this.rows.isEmpty()) return 0;
			return rows.stream().filter(v -> v > 0).mapToInt(Integer::intValue).sum() + this.gap * (this.rows.size() - 1);
		}

		public Builder setOffset(int x, int y) {
			this.offsetX = x;
			this.offsetY = y;
			return this;
		}

		public Builder centerAround(int x, int y) {
			this.setOffset(x - this.getWidth() / 2, y - this.getHeight() / 2);
			return this;
		}

		public Builder setLimit(int x, int y) {
			this.limitX = x;
			this.limitY = y;
			return this;
		}

		public Builder render(Consumer<GridLayout> action) {
			this.deferredActions.add(action);
			return this;
		}

		public Builder renderRow(Consumer<GridLayout> action) {
			var index = Math.max(0, this.rows.size() - 1) * Math.max(1, this.columns.size());
			this.deferredActions.add(layout -> {
				layout.index = index;
				layout.current = layout.cells.get(layout.index);
				action.accept(layout);
			});
			return this;
		}

		public Builder renderColumn(Consumer<GridLayout> action) {
			var index = Math.max(1, this.rows.size()) * Math.max(0, this.columns.size() - 1);
			this.deferredActions.add(layout -> {
				layout.index = index;
				layout.current = layout.cells.get(layout.index);
				action.accept(layout);
			});
			return this;
		}

		public GridLayout build() {
			var cells = new ArrayList<Cell>();

			int growColumnWidth = this.growColumns == 0 ? 0 : (this.limitX - this.getWidth()) / this.growColumns;
			int growRowHeight = this.growRows == 0 ? 0 : (this.limitY - this.getHeight()) / this.growRows;

			int offsetY = this.offsetY;
			for (int row = 0; row < this.rows.size(); row++) {
				int rowHeight = this.rows.get(row);
				if (rowHeight == Integer.MIN_VALUE) rowHeight = growRowHeight;

				int offsetX = this.offsetX;
				for (int column = 0; column < this.columns.size(); column++) {
					int columnWidth = this.columns.get(column);
					if (columnWidth == Integer.MIN_VALUE) columnWidth = growColumnWidth;

					cells.add(new Cell(offsetX, offsetY, columnWidth, rowHeight));

					offsetX += columnWidth + this.gap;
				}

				offsetY += rowHeight + this.gap;
			}

			var layout = new GridLayout(this.columns.size(), this.rows.size(), cells);

			for (var action : this.deferredActions) {
				action.accept(layout);
			}

			return layout;
		}
	}

	public static Builder builder() {
		return new Builder();
	}

	public record Cell(int x, int y, int width, int height) {
		public Builder childLayout() {
			return new Builder().setOffset(this.x, this.y).setLimit(this.width, this.height);
		}
	}

	protected GridLayout(int column, int rows, List<Cell> cells) {
		this.cells = cells;
		this.columns = column;
		this.rows = rows;

		this.current = this.cells.get(0);
	}

	protected final List<Cell> cells;
	protected final int columns;
	protected final int rows;

	protected int index = 0;
	protected Cell current = null;

	public Cell cell() {
		return current;
	}

	public GridLayout apply(AbstractWidget target) {
		target.setRectangle(this.current.width, this.current.height, this.current.x, this.current.y);
		this.next();
		return this;
	}

	public GridLayout next() {
		return this.next(1);
	}

	public GridLayout next(int offset) {
		this.index += offset;
		this.current = this.index < this.cells.size() ? this.cells.get(this.index) : null;
		return this;
	}

	public GridLayout nextRow() {
		return this.nextRow(1);
	}

	public GridLayout nextRow(int offset) {
		var row = this.index / this.columns;
		this.index = (row + offset) * this.columns;
		this.current = this.cells.get(this.index);
		return this;
	}

	public GridLayout colspan() {
		return this.colspan(this.columns);
	}

	public GridLayout colspan(int colspan) {
		this.index += colspan - 1;
		var endCell = this.cells.get(this.index);
		this.current = new Cell(this.current.x, this.current.y, endCell.x + endCell.width - this.current.x, this.current.height);
		return this;
	}
}
