package xfacthd.ghwebhookserver.util;

import java.util.Objects;

public final class Either<LEFT, RIGHT>
{
    private final LEFT left;
    private final RIGHT right;

    private Either(LEFT left, RIGHT right)
    {
        this.left = left;
        this.right = right;
    }

    public boolean hasLeft() { return left != null; }

    public boolean hasRight() { return right != null; }

    public LEFT getLeft() { return left; }

    public RIGHT getRight() { return right; }



    public static <L, R> Either<L, R> left(L left)
    {
        Objects.requireNonNull(left);
        return new Either<>(left, null);
    }

    public static <L, R> Either<L, R> right(R right)
    {
        Objects.requireNonNull(right);
        return new Either<>(null, right);
    }
}
